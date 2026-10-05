/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache;

import io.micronaut.cache.annotation.CacheAnnotation;
import io.micronaut.cache.interceptor.CacheInterceptor;
import io.micronaut.cache.interceptor.CacheKeyGenerator;
import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentMode;
import io.micronaut.context.reload.ClassChange;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanDefinitionReference;
import io.micronaut.inject.ExecutableMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Keeps the caches and the cache interceptor in step with the code in development mode. It exists only in
 * development mode, so nothing of it is on the path of a cached invocation.
 *
 * <ul>
 *     <li>A class change applied in place that retires a classloader, or that redefines a class with cache
 *     operations or a cache key generator, invalidates every cache: the values were computed by the code it
 *     replaces, and can hold instances of the retired classes.</li>
 *     <li>A class change applied in place that retires a classloader also recreates the cache interceptors,
 *     whose operation and key generator maps are keyed by the methods and classes of the retired generation.
 *     The advised beans that hold an interceptor are destroyed with it, as the dependency graph records, and
 *     are created again on top of the new one.</li>
 * </ul>
 *
 * <p>A change that restarts the application is ignored: the new context has new caches. Each bean is recreated
 * through {@link WatchableBeanContext#recreate(Object)}; a context that does not track bean dependencies recreates
 * nothing, and the beans are kept, rather than replaced under the beans that received them. The caches are still
 * invalidated.</p>
 *
 * <p>It holds the context only, never a cache bean: a bean that received one is a dependent of it, which
 * recreating it would destroy along with its watches.</p>
 *
 * @author graemerocher
 * @since 6.2.0
 */
@Internal
@Context
@Requires(condition = DevelopmentMode.Active.class)
final class DevelopmentCacheReloader {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentCacheReloader.class);

    /**
     * How long a reload waits for an asynchronous cache to be invalidated.
     */
    private static final Duration INVALIDATION_TIMEOUT = Duration.ofSeconds(30);

    private final BeanContext beanContext;

    /**
     * @param beanContext The context, watched when it can be
     */
    DevelopmentCacheReloader(BeanContext beanContext) {
        this.beanContext = beanContext;
        if (beanContext instanceof WatchableBeanContext watchable) {
            watchable.watchClassChanges(this::onClassChange);
        }
    }

    private void onClassChange(ClassChangeEvent change) {
        if (change.strategy() == ReloadStrategy.RESTART) {
            return;
        }
        if (!change.retiredLoaders().isEmpty()) {
            invalidateAll("a reload retired a classloader");
            recreate(CacheInterceptor.class, "a reload retired a classloader");
            return;
        }
        for (ClassChange classChange : change.changes()) {
            String className = classChange.className();
            if (isCached(className, change.newLoader()) || wasCached(className)) {
                invalidateAll(className + " changed");
                return;
            }
        }
    }

    /**
     * Whether the new class has cache operations, on itself or its methods, or generates cache keys.
     *
     * @param className The changed class
     * @param loader The loader of the new generation
     * @return Whether cached values may have been computed from what it replaces
     */
    private static boolean isCached(String className, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            if (CacheKeyGenerator.class.isAssignableFrom(type) || anyCacheAnnotation(type.getAnnotations())) {
                return true;
            }
            for (Method method : type.getDeclaredMethods()) {
                if (anyCacheAnnotation(method.getAnnotations())) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException | LinkageError e) {
            // removed, or not loadable on its own: nothing of the new generation was cached from it
            return false;
        }
    }

    private static boolean anyCacheAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(CacheAnnotation.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the class a change replaces had cache operations, as the context was compiled: a bean definition of
     * it, or of its proxy, carries a cache annotation on the class or an executable method. The references are
     * matched by name, so that only the definitions of that class are loaded, and nothing of a previous class is kept.
     *
     * @param className The changed class
     * @return Whether the interceptor cached results of the class it replaces
     */
    private boolean wasCached(String className) {
        int lastDot = className.lastIndexOf('.');
        String definition = className.substring(0, lastDot + 1) + '$' + className.substring(lastDot + 1) + "$Definition";
        for (BeanDefinitionReference<?> reference : beanContext.getBeanDefinitionReferences()) {
            String name = reference.getBeanDefinitionName();
            if ((name.equals(definition) || name.startsWith(definition + '$')) && hasCacheOperations(reference)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCacheOperations(BeanDefinitionReference<?> reference) {
        try {
            if (hasCacheAnnotation(reference.getAnnotationMetadata())) {
                return true;
            }
            BeanDefinition<?> definition = reference.load();
            if (definition == null) {
                return false;
            }
            if (hasCacheAnnotation(definition.getAnnotationMetadata())) {
                return true;
            }
            for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
                if (hasCacheAnnotation(method.getAnnotationMetadata())) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException | LinkageError e) {
            // a definition of that name that no longer loads: what it was is unknown, so it counts
            return true;
        }
    }

    private static boolean hasCacheAnnotation(AnnotationMetadata metadata) {
        return metadata.hasStereotype(CacheAnnotation.class);
    }

    /**
     * Invalidates every cache the context holds: the caches of each cache manager, which include the ones a
     * dynamic cache manager created on demand, and the cache beans. A manager or a cache nobody asked for yet
     * holds nothing.
     *
     * @param reason Why, for the log
     */
    private void invalidateAll(String reason) {
        List<Object> caches = new ArrayList<>();
        for (BeanRegistration<CacheManager> registration : beanContext.getActiveBeanRegistrations(CacheManager.class)) {
            CacheManager<?> manager = registration.bean();
            for (String name : List.copyOf(manager.getCacheNames())) {
                try {
                    add(caches, manager.getCache(name));
                } catch (RuntimeException e) {
                    LOG.debug("Cache {} not resolved for invalidation: {}", name, e.getMessage(), e);
                }
            }
        }
        for (BeanRegistration<SyncCache> registration : beanContext.getActiveBeanRegistrations(SyncCache.class)) {
            add(caches, registration.bean());
        }
        for (BeanRegistration<AsyncCache> registration : beanContext.getActiveBeanRegistrations(AsyncCache.class)) {
            add(caches, registration.bean());
        }
        if (caches.isEmpty()) {
            return;
        }
        LOG.debug("Invalidating {} caches: {}", caches.size(), reason);
        for (Object cache : caches) {
            try {
                if (cache instanceof SyncCache<?> sync) {
                    sync.invalidateAll();
                } else if (cache instanceof AsyncCache<?> async) {
                    // waited for: the reload completes only once nothing of the retired code can be served from the cache
                    async.invalidateAll().get(INVALIDATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException | ExecutionException | TimeoutException e) {
                LOG.warn("Cache {} could not be invalidated: {}", ((Cache<?>) cache).getName(), e.getMessage(), e);
            }
        }
    }

    private static void add(List<Object> caches, Object cache) {
        for (Object taken : caches) {
            if (taken == cache) {
                return;
            }
        }
        caches.add(cache);
    }

    /**
     * Recreates the singletons of a type the context holds, and the beans that received them. Nothing is created
     * that was not created already.
     *
     * @param type The bean type
     * @param reason Why, for the log
     */
    private void recreate(Class<?> type, String reason) {
        if (!(beanContext instanceof WatchableBeanContext context)) {
            return;
        }
        // taken first: recreating one destroys the beans that received it, as the graph records them
        List<Object> beans = new ArrayList<>();
        for (BeanRegistration<?> registration : beanContext.getActiveBeanRegistrations(type)) {
            add(beans, registration.bean());
        }
        if (beans.isEmpty()) {
            return;
        }
        LOG.debug("Recreating {}: {}", type.getSimpleName(), reason);
        for (Object bean : beans) {
            // false for a bean destroyed with one recreated before it, and for all of them in a context that does
            // not track bean dependencies: they are kept, and read again after a restart
            context.recreate(bean);
        }
    }
}
