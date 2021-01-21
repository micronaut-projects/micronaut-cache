/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.cache.interceptor;

import io.micronaut.aop.InterceptPhase;
import io.micronaut.aop.InterceptedMethod;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.AsyncCacheErrorHandler;
import io.micronaut.cache.CacheErrorHandler;
import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.annotation.*;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueResolver;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.reflect.InstantiationUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.scheduling.TaskExecutors;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * <p>An AOP {@link MethodInterceptor} implementation for the Cache annotations {@link Cacheable},
 * {@link CachePut} and {@link CacheInvalidate}.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Singleton
public class CacheInterceptor implements MethodInterceptor<Object, Object> {
    /**
     * The position on the interceptor in the chain.
     */
    public static final int POSITION = InterceptPhase.CACHE.getPosition();

    private static final String MEMBER_CACHE_NAMES = "cacheNames";
    private static final String MEMBER_ASYNC = "async";
    private static final Logger LOG = LoggerFactory.getLogger(CacheInterceptor.class);
    private static final String MEMBER_ATOMIC = "atomic";
    private static final String MEMBER_PARAMETERS = "parameters";
    private static final String MEMBER_ALL = "all";
    private static final String MEMBER_KEY_GENERATOR = "keyGenerator";

    private final CacheManager cacheManager;
    private final Map<Class<? extends CacheKeyGenerator>, CacheKeyGenerator> keyGenerators = new ConcurrentHashMap<>();
    private final Map<ExecutableMethod<?, ?>, CacheOperation> cacheOperations = new ConcurrentHashMap<>(30);
    private final BeanContext beanContext;
    private final ExecutorService ioExecutor;
    private final CacheErrorHandler errorHandler;
    private final AsyncCacheErrorHandler asyncCacheErrorHandler;

    /**
     * Create Cache Interceptor with given arguments.
     *
     * @param cacheManager           The cache manager
     * @param errorHandler           Cache error handler
     * @param asyncCacheErrorHandler Async cache error handlers
     * @param ioExecutor             The executor to create tasks
     * @param beanContext            The bean context to allow DI
     */
    public CacheInterceptor(CacheManager cacheManager,
                            CacheErrorHandler errorHandler,
                            AsyncCacheErrorHandler asyncCacheErrorHandler,
                            @Named(TaskExecutors.IO) ExecutorService ioExecutor,
                            BeanContext beanContext) {
        this.cacheManager = cacheManager;
        this.errorHandler = errorHandler;
        this.asyncCacheErrorHandler = asyncCacheErrorHandler;
        this.beanContext = beanContext;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        if (context.hasStereotype(CacheAnnotation.class)) {
            InterceptedMethod interceptedMethod = InterceptedMethod.of(context);
            try {
                ReturnType<?> returnType = context.getReturnType();
                Argument<?> returnTypeValue = interceptedMethod.returnTypeValue();
                switch (interceptedMethod.resultType()) {
                    case COMPLETION_STAGE:
                        CompletionStage<?> completionStage = interceptAsCompletableFuture(context,
                                interceptedMethod::interceptResultAsCompletionStage,
                                returnType,
                                returnTypeValue);
                        return interceptedMethod.handleResult(completionStage);
                    case PUBLISHER:
                        Supplier<CompletionStage<?>> supplier = () -> {
                            Flowable<Object> flowable = Publishers.convertPublisher(context.proceed(), Flowable.class);
                            return toCompletableFuture(flowable);
                        };
                        CompletionStage<?> result = interceptAsCompletableFuture(context, supplier, returnType, returnTypeValue);
                        Object publisherResult = Publishers.convertPublisher(result, returnType.getType());
                        return interceptedMethod.handleResult(publisherResult);
                    case SYNCHRONOUS:
                        return interceptSync(context, returnType);
                    default:
                        return interceptedMethod.unsupported();
                }
            } catch (Exception e) {
                return interceptedMethod.handleException(e);
            }
        } else {
            return context.proceed();
        }
    }

    private CompletableFuture<Object> toCompletableFuture(Flowable<Object> flowable) {
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        flowable.firstElement().subscribe(completableFuture::complete, completableFuture::completeExceptionally, () -> completableFuture.complete(null));
        return completableFuture;
    }

    /**
     * Intercept the annotated method invocation with sync.
     *
     * @param context    Contains information about method invocation
     * @param returnType The return type class
     * @return The value from the cache
     */
    protected Object interceptSync(MethodInvocationContext context, ReturnType<?> returnType) {
        final ValueWrapper wrapper = new ValueWrapper();
        CacheOperation cacheOperation = getCacheOperation(context, returnType.isVoid());

        if (cacheOperation.cacheable) {
            Object key = getCacheableKey(context, cacheOperation);
            Argument returnArgument = returnType.asArgument();
            if (context.isTrue(Cacheable.class, MEMBER_ATOMIC)) {
                SyncCache syncCache = cacheManager.getCache(cacheOperation.cacheableCacheName);

                try {
                    wrapper.value = syncCache.get(key, returnArgument, () -> {
                        try {
                            doProceed(context, wrapper);
                            return wrapper.value;
                        } catch (RuntimeException e) {
                            throw new ValueSupplierException(key, e);
                        }
                    });
                } catch (ValueSupplierException e) {
                    throw e.getCause();
                } catch (RuntimeException e) {
                    errorHandler.handleLoadError(syncCache, key, e);
                    throw e;
                }
            } else {
                String[] cacheNames = resolveCacheNames(
                        cacheOperation.defaultCacheNames,
                        context.stringValues(Cacheable.class, MEMBER_CACHE_NAMES)
                );
                boolean cacheHit = false;
                for (String cacheName : cacheNames) {
                    SyncCache syncCache = cacheManager.getCache(cacheName);
                    try {
                        Optional optional = syncCache.get(key, returnArgument);
                        if (optional.isPresent()) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Value found in cache [" + cacheName + "] for invocation: " + context);
                            }
                            cacheHit = true;
                            wrapper.value = optional.get();
                            break;
                        }
                    } catch (RuntimeException e) {
                        if (errorHandler.handleLoadError(syncCache, key, e)) {
                            throw e;
                        }
                    }
                }
                if (!cacheHit) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Value not found in cache for invocation: " + context);
                    }
                    doProceed(context, wrapper);
                    syncPut(cacheNames, key, wrapper.value);
                }
            }
        } else {
            if (!cacheOperation.hasWriteOperations()) {
                return context.proceed();
            } else {
                doProceed(context, wrapper);
            }
        }

        List<AnnotationValue<CachePut>> cachePuts = cacheOperation.putOperations;
        if (cachePuts != null) {
            for (AnnotationValue<CachePut> cachePut : cachePuts) {
                processCachePut(context, wrapper, cachePut, cacheOperation);
            }
        }

        List<AnnotationValue<CacheInvalidate>> cacheInvalidates = cacheOperation.invalidateOperations;
        if (cacheInvalidates != null) {
            for (AnnotationValue<CacheInvalidate> cacheInvalidate : cacheInvalidates) {
                processCacheEvict(context, cacheOperation, cacheInvalidate);
            }
        }

        return wrapper.optional ? Optional.ofNullable(wrapper.value) : wrapper.value;
    }

    /**
     * Intercept the async method invocation.
     *
     * @param context          Contains information about method invocation
     * @param intercept        The intercepted result
     * @param returnTypeObject The return type of the method in Micronaut
     * @param requiredType     The return type class
     * @return The value from the cache
     */
    protected CompletionStage<?> interceptAsCompletableFuture(MethodInvocationContext<Object, Object> context, Supplier<CompletionStage<?>> intercept, ReturnType<?> returnTypeObject, Argument<?> requiredType) {
        CacheOperation cacheOperation = getCacheOperation(context, returnTypeObject.isVoid() || requiredType.equalsType(Argument.VOID_OBJECT));
        CompletionStage<?> returnFuture;
        if (cacheOperation.cacheable) {
            AsyncCache<?> asyncCache = cacheManager.getCache(cacheOperation.cacheableCacheName).async();
            Object key = getCacheableKey(context, cacheOperation);
            returnFuture = asyncCacheGet(asyncCache, key, requiredType, errorHandler)
                    .thenCompose(o -> {
                        if (o.isPresent()) {
                            // cache hit, return result
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Value found in cache [" + asyncCache.getName() + "] for invocation: " + context);
                            }
                            return CompletableFuture.completedFuture(o.get());
                        } else {
                            // cache miss proceed with original future
                            CompletionStage<?> completableFuture = intercept.get();
                            if (completableFuture == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return completableFuture.thenCompose(o1 -> {
                                if (o1 == null) {
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace("Invalidating the key [{}] of the cache [{}] since the result of invocation [{}] was null", key, asyncCache.getName(), context);
                                    }
                                    return asyncCacheInvalidate(asyncCache, key, errorHandler).thenApply(ignore -> null);
                                } else {
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace("Storing in the cache [{}] with key [{}] the result of invocation [{}]: {}", asyncCache.getName(), key, context, o1);
                                    }
                                    return asyncCachePut(asyncCache, key, o1, errorHandler).thenApply(ignore -> o1);
                                }
                            });
                        }
                    }).toCompletableFuture();
        } else {
            returnFuture = intercept.get();
        }
        if (cacheOperation.hasWriteOperations()) {
            returnFuture = processFuturePutOperations(context, cacheOperation, returnFuture);
            returnFuture = processFutureInvalidateOperations(context, cacheOperation, returnFuture);
        }
        return returnFuture;
    }

    /**
     * Saving inside the cache.
     *
     * @param method Contains information about method invocation
     * @return The operations to cause the return value to be cached within the given cache name.
     */
    protected List<AnnotationValue<CachePut>> putOperations(ExecutableMethod<?, ?> method) {
        return method.getAnnotationValuesByType(CachePut.class);
    }

    /**
     * Evict from the cache.
     *
     * @param method Contains information about method invocation
     * @return The operations to cause the eviction of the given caches
     */
    protected List<AnnotationValue<CacheInvalidate>> invalidateOperations(ExecutableMethod<?, ?> method) {
        return method.getAnnotationValuesByType(CacheInvalidate.class);
    }

    private CacheOperation getCacheOperation(MethodInvocationContext<Object, Object> context, boolean isVoid) {
        ExecutableMethod<Object, Object> method = context.getExecutableMethod();
        CacheOperation cacheOperation = cacheOperations.get(method);
        if (cacheOperation == null) {
            cacheOperation = new CacheOperation(method, isVoid);
            cacheOperations.put(method, cacheOperation);
        }
        return cacheOperation;
    }

    private CompletionStage<?> processFuturePutOperations(MethodInvocationContext<Object, Object> context,
                                                          CacheOperation cacheOperation,
                                                          CompletionStage<?> value) {
        List<AnnotationValue<CachePut>> putOperations = cacheOperation.putOperations;
        if (putOperations != null) {
            for (AnnotationValue<CachePut> putOperation : putOperations) {
                String[] cacheNames = cacheOperation.getCachePutNames(putOperation);
                if (ArrayUtils.isNotEmpty(cacheNames)) {
                    boolean isAsync = putOperation.isTrue(MEMBER_ASYNC);
                    if (isAsync) {
                        value.whenCompleteAsync((result, throwable) -> {
                            if (throwable == null) {
                                putAsync(context, cacheOperation, putOperation, cacheNames, result, asyncCacheErrorHandler);
                            }
                        }, ioExecutor);
                    } else {
                        return value.thenCompose(result -> putAsync(context, cacheOperation, putOperation, cacheNames, result, errorHandler));
                    }
                }
            }
        }
        return value;
    }

    private CompletionStage<?> processFutureInvalidateOperations(MethodInvocationContext<Object, Object> context,
                                                                 CacheOperation cacheOperation,
                                                                 CompletionStage<?> value) {
        List<AnnotationValue<CacheInvalidate>> invalidateOperations = cacheOperation.invalidateOperations;
        if (invalidateOperations != null) {
            for (AnnotationValue<CacheInvalidate> invalidateOperation : invalidateOperations) {
                String[] cacheNames = cacheOperation.getCacheInvalidateNames(invalidateOperation);
                if (ArrayUtils.isNotEmpty(cacheNames)) {
                    boolean isAsync = invalidateOperation.isTrue(MEMBER_ASYNC);
                    if (isAsync) {
                        value.whenCompleteAsync((result, throwable) -> {
                            if (throwable == null) {
                                invalidateAsync(context, cacheOperation, invalidateOperation, cacheNames, asyncCacheErrorHandler);
                            }
                        }, ioExecutor);
                    } else {
                        return value.thenCompose(result -> invalidateAsync(context, cacheOperation, invalidateOperation, cacheNames, errorHandler)
                                .thenApply(ignore -> result));
                    }
                }
            }
        }
        return value;
    }

    private CompletableFuture<Object> putAsync(MethodInvocationContext context,
                                               CacheOperation cacheOperation,
                                               AnnotationValue<CachePut> putOperation,
                                               String[] cacheNames,
                                               Object value,
                                               CacheErrorHandler errorHandler) {
        Object key = getOperationKey(context, putOperation, cacheOperation.getCachePutKeyGenerator(putOperation));
        if (value == null) {
            return buildInvalidateFutures(cacheNames, key, errorHandler).thenApply(ignore -> null);
        }
        return buildPutFutures(cacheNames, key, value, errorHandler).thenApply(ignore -> value);
    }

    private CompletableFuture<Boolean> invalidateAsync(MethodInvocationContext context,
                                                       CacheOperation cacheOperation,
                                                       AnnotationValue<CacheInvalidate> invalidateOperation,
                                                       String[] cacheNames,
                                                       CacheErrorHandler errorHandler) {
        boolean invalidateAll = invalidateOperation.isTrue(MEMBER_ALL);
        if (invalidateAll) {
            return buildInvalidateAllFutures(cacheNames, errorHandler);
        } else {
            CacheKeyGenerator keyGenerator = cacheOperation.getCacheInvalidateKeyGenerator(invalidateOperation);
            Object key = getOperationKey(context, invalidateOperation, keyGenerator);
            return buildInvalidateFutures(cacheNames, key, errorHandler);
        }
    }

    private Object getCacheableKey(MethodInvocationContext context, CacheOperation cacheOperation) {
        CacheKeyGenerator keyGenerator = resolveKeyGenerator(cacheOperation.defaultKeyGenerator, context.classValue(Cacheable.class, MEMBER_KEY_GENERATOR).orElse(null));
        Object[] parameterValues = resolveParams(context, context.stringValues(Cacheable.class, MEMBER_PARAMETERS));
        return keyGenerator.generateKey(context, parameterValues);
    }

    private Object getOperationKey(MethodInvocationContext context, AnnotationValueResolver annotationValueResolver, CacheKeyGenerator keyGenerator) {
        String[] parameterNames = annotationValueResolver.stringValues(MEMBER_PARAMETERS);
        Object[] parameterValues = resolveParams(context, parameterNames);
        return keyGenerator.generateKey(context, parameterValues);
    }

    /**
     * Resolve the cache key generator from the give type.
     *
     * @param type The key generator
     * @return The cache key generator
     */
    protected CacheKeyGenerator resolveKeyGenerator(Class<? extends CacheKeyGenerator> type) {
        if (type == null) {
            type = DefaultCacheKeyGenerator.class;
        }
        return keyGenerators.computeIfAbsent(type, aClass -> {
            Optional<? extends CacheKeyGenerator> cacheKeyGenerator = beanContext.findBean(aClass);
            if (cacheKeyGenerator.isPresent()) {
                return cacheKeyGenerator.get();
            }
            return InstantiationUtils.instantiate(aClass);
        });
    }

    private CompletableFuture<Boolean> buildPutFutures(String[] cacheNames, Object key, Object value, CacheErrorHandler errorHandler) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (String cacheName : cacheNames) {
            AsyncCache<?> asyncCache = cacheManager.getCache(cacheName).async();
            futures.add(asyncCachePut(asyncCache, key, value, errorHandler));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(ignore -> true);
    }

    private CompletableFuture<Boolean> buildInvalidateFutures(String[] cacheNames, Object key, CacheErrorHandler errorHandler) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(cacheNames.length);
        for (String cacheName : cacheNames) {
            AsyncCache<?> asyncCache = cacheManager.getCache(cacheName).async();
            futures.add(asyncCacheInvalidate(asyncCache, key, errorHandler));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(ignore -> true);
    }

    private CompletableFuture<Boolean> buildInvalidateAllFutures(String[] cacheNames, CacheErrorHandler errorHandler) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(cacheNames.length);
        for (String cacheName : cacheNames) {
            AsyncCache<?> asyncCache = cacheManager.getCache(cacheName).async();
            futures.add(asyncCacheInvalidateAll(asyncCache, errorHandler));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(ignore -> true);
    }

    private CacheKeyGenerator resolveKeyGenerator(CacheKeyGenerator defaultKeyGenerator, Class type) {
        CacheKeyGenerator keyGenerator = defaultKeyGenerator;
        @SuppressWarnings("unchecked")
        Class<? extends CacheKeyGenerator> alternateKeyGen = type != null && CacheKeyGenerator.class.isAssignableFrom(type) ? type : null;
        if (alternateKeyGen != null && keyGenerator.getClass() != alternateKeyGen) {
            keyGenerator = resolveKeyGenerator(alternateKeyGen);
        }
        if (keyGenerator == null) {
            return new DefaultCacheKeyGenerator();
        }
        return keyGenerator;

    }

    private String[] resolveCacheNames(String[] defaultCacheNames, String[] cacheNames) {
        if (ArrayUtils.isEmpty(cacheNames)) {
            cacheNames = defaultCacheNames;
        }
        return cacheNames;
    }

    private void doProceed(MethodInvocationContext context, ValueWrapper wrapper) {
        Object result = context.proceed();
        if (result instanceof Optional) {
            Optional optional = (Optional) result;
            wrapper.optional = true;
            if (optional.isPresent()) {
                wrapper.value = optional.get();
            }
        } else {
            wrapper.value = result;
        }
    }

    private void processCachePut(MethodInvocationContext<?, ?> context, ValueWrapper wrapper, AnnotationValue<CachePut> cacheConfig, CacheOperation cacheOperation) {
        String[] cacheNames = cacheOperation.getCachePutNames(cacheConfig);
        if (!ArrayUtils.isEmpty(cacheNames)) {
            boolean isAsync = cacheConfig.isTrue(MEMBER_ASYNC);
            Object value = wrapper.value;
            if (isAsync) {
                ioExecutor.submit(() -> {
                    CacheKeyGenerator keyGenerator = cacheOperation.getCachePutKeyGenerator(cacheConfig);
                    Object key = getOperationKey(context, cacheConfig, keyGenerator);
                    if (value == null) {
                        buildInvalidateFutures(cacheNames, key, asyncCacheErrorHandler);
                    } else {
                        buildPutFutures(cacheNames, key, value, asyncCacheErrorHandler);
                    }
                });
            } else {
                CacheKeyGenerator keyGenerator = cacheOperation.getCachePutKeyGenerator(cacheConfig);
                Object key = getOperationKey(context, cacheConfig, keyGenerator);
                syncPut(cacheNames, key, value);
            }
        }
    }

    private void syncPut(String[] cacheNames, Object key, Object value) {
        for (String cacheName : cacheNames) {
            SyncCache syncCache = cacheManager.getCache(cacheName);
            try {
                if (value == null) {
                    syncCache.invalidate(key);
                } else {
                    syncCache.put(key, value);
                }
            } catch (RuntimeException e) {
                if (errorHandler.handlePutError(syncCache, key, value, e)) {
                    throw e;
                }
            }
        }
    }

    private void processCacheEvict(MethodInvocationContext context,
                                   CacheOperation cacheOperation,
                                   AnnotationValue<CacheInvalidate> cacheInvalidate) {
        String[] cacheNames = cacheOperation.getCacheInvalidateNames(cacheInvalidate);
        if (!ArrayUtils.isEmpty(cacheNames)) {
            boolean isAsync = cacheInvalidate.isTrue(MEMBER_ASYNC);
            if (isAsync) {
                ioExecutor.submit(() -> invalidateAsync(context, cacheOperation, cacheInvalidate, cacheNames, asyncCacheErrorHandler));
            } else {
                invalidateSync(context, cacheOperation, cacheInvalidate, cacheNames);
            }
        }
    }

    private void invalidateSync(MethodInvocationContext context, CacheOperation cacheOperation, AnnotationValue<CacheInvalidate> cacheConfig, String[] cacheNames) {
        boolean invalidateAll = cacheConfig.isTrue(MEMBER_ALL);
        for (String cacheName : cacheNames) {
            SyncCache syncCache = cacheManager.getCache(cacheName);
            if (invalidateAll) {
                try {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Invalidating all the entries of the cache [{}]", syncCache.getName());
                    }
                    syncCache.invalidateAll();
                } catch (RuntimeException e) {
                    if (errorHandler.handleInvalidateError(syncCache, e)) {
                        throw e;
                    }
                }
            } else {
                CacheKeyGenerator keyGenerator = cacheOperation.getCacheInvalidateKeyGenerator(cacheConfig);
                Object key = getOperationKey(context, cacheConfig, keyGenerator);
                try {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Invalidating the key [{}] of the cache [{}]", key, syncCache.getName());
                    }
                    syncCache.invalidate(key);
                } catch (RuntimeException e) {
                    if (errorHandler.handleInvalidateError(syncCache, key, e)) {
                        throw e;
                    }
                }
            }
        }
    }

    private CompletableFuture<? extends Optional<?>> asyncCacheGet(AsyncCache<?> asyncCache, Object key, Argument<?> requiredType, CacheErrorHandler errorHandler) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting the value for the key [{}] of the cache [{}]", key, asyncCache.getName());
        }
        return asyncCache.get(key, requiredType)
                .exceptionally(throwable ->
                        exceptionallyAsync(throwable, () -> errorHandler.handleLoadError(asyncCache, key, asRuntimeException(throwable)), null));
    }

    private CompletableFuture<Boolean> asyncCachePut(AsyncCache<?> asyncCache, Object key, Object value, CacheErrorHandler errorHandler) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Putting the value [{}] for the key [{}] of the cache [{}]", value, key, asyncCache.getName());
        }
        return asyncCache.put(key, value).exceptionally(throwable ->
                exceptionallyAsync(throwable, () -> errorHandler.handlePutError(asyncCache, key, value, asRuntimeException(throwable)), true));
    }

    private CompletableFuture<Boolean> asyncCacheInvalidate(AsyncCache<?> asyncCache, Object key, CacheErrorHandler errorHandler) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Invalidating the key [{}] of the cache [{}]", key, asyncCache.getName());
        }
        return asyncCache.invalidate(key).exceptionally(throwable ->
                exceptionallyAsync(throwable, () -> errorHandler.handleInvalidateError(asyncCache, key, asRuntimeException(throwable)), true));
    }

    private CompletableFuture<Boolean> asyncCacheInvalidateAll(AsyncCache<?> asyncCache, CacheErrorHandler errorHandler) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Invalidating all the entries of the cache [{}]", asyncCache.getName());
        }
        return asyncCache.invalidateAll().exceptionally(throwable ->
                exceptionallyAsync(throwable, () -> errorHandler.handleInvalidateError(asyncCache, asRuntimeException(throwable)), true));
    }

    private <T> T exceptionallyAsync(Throwable throwable, Supplier<Boolean> handleInvalidateErrorSupplier, T def) {
        // replace with exceptionallyAsync in Java 11
        boolean handleInvalidateError = true;
        try {
            handleInvalidateError = ioExecutor.submit(handleInvalidateErrorSupplier::get).get();
        } catch (Throwable e) {
            // Ignore
        }
        if (handleInvalidateError) {
            Throwable rethrow = throwable;
            if (rethrow instanceof CompletionException) {
                rethrow = throwable.getCause();
            }
            throw new CompletionException(rethrow);
        }
        return def;
    }

    private RuntimeException asRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        } else {
            return new RuntimeException(throwable);
        }
    }

    private Object[] resolveParams(MethodInvocationContext<?, ?> context, String[] parameterNames) {
        Object[] parameterValues;
        Object[] methodParameterValues = context.getParameterValues();
        if (ArrayUtils.isEmpty(parameterNames)) {
            parameterValues = methodParameterValues;
        } else {
            List<Object> list = new ArrayList<>();
            Set<String> names = CollectionUtils.setOf(parameterNames);
            Argument<?>[] arguments = context.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                Argument<?> argument = arguments[i];
                if (names.contains(argument.getName())) {
                    list.add(methodParameterValues[i]);
                }
            }
            parameterValues = list.toArray();
        }
        return parameterValues;
    }

    /**
     *
     */
    private class CacheOperation {
        final CacheKeyGenerator defaultKeyGenerator;
        final String[] defaultCacheNames;
        final boolean cacheable;
        String cacheableCacheName;
        List<AnnotationValue<CachePut>> putOperations;
        List<AnnotationValue<CacheInvalidate>> invalidateOperations;

        CacheOperation(ExecutableMethod<?, ?> method, boolean isVoid) {
            this.defaultKeyGenerator = resolveKeyGenerator(
                    method.classValue(CacheConfig.class, MEMBER_KEY_GENERATOR).orElse(null)
            );
            this.putOperations = isVoid ? null : putOperations(method);
            this.invalidateOperations = invalidateOperations(method);
            this.defaultCacheNames = method.stringValues(CacheConfig.class, MEMBER_CACHE_NAMES);
            this.cacheable = method.hasStereotype(Cacheable.class);
            if (!isVoid && cacheable) {
                String[] names = resolveCacheNames(defaultCacheNames, method.stringValues(Cacheable.class, MEMBER_CACHE_NAMES));
                if (ArrayUtils.isNotEmpty(names)) {
                    this.cacheableCacheName = names[0];
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("No cache names defined for invocation [{}]. Skipping cache read operations.", method);
                    }
                }
            }
        }

        boolean hasWriteOperations() {
            return putOperations != null || invalidateOperations != null;
        }

        String[] getCachePutNames(AnnotationValue<CachePut> cacheConfig) {
            return getCacheNames(cacheConfig.stringValues(MEMBER_CACHE_NAMES));
        }

        String[] getCacheInvalidateNames(AnnotationValue<CacheInvalidate> cacheConfig) {
            return getCacheNames(cacheConfig.stringValues(MEMBER_CACHE_NAMES));
        }

        CacheKeyGenerator getCacheInvalidateKeyGenerator(AnnotationValue<CacheInvalidate> cacheConfig) {
            return cacheConfig.get(MEMBER_KEY_GENERATOR, CacheKeyGenerator.class).orElseGet(() ->
                    getKeyGenerator(cacheConfig.classValue(MEMBER_KEY_GENERATOR).orElse(null))
            );
        }

        CacheKeyGenerator getCachePutKeyGenerator(AnnotationValue<CachePut> cacheConfig) {
            return cacheConfig.get(MEMBER_KEY_GENERATOR, CacheKeyGenerator.class).orElseGet(() ->
                    getKeyGenerator(cacheConfig.classValue(MEMBER_KEY_GENERATOR).orElse(null))
            );
        }

        private String[] getCacheNames(String[] cacheNames) {
            if (ArrayUtils.isEmpty(cacheNames)) {
                return defaultCacheNames;
            } else {
                return cacheNames;
            }
        }

        private CacheKeyGenerator getKeyGenerator(Class<?> alternateKeyGen) {
            CacheKeyGenerator keyGenerator = defaultKeyGenerator;
            if (alternateKeyGen != null && defaultKeyGenerator.getClass() != alternateKeyGen && CacheKeyGenerator.class.isAssignableFrom(alternateKeyGen)) {
                //noinspection unchecked
                keyGenerator = resolveKeyGenerator((Class<? extends CacheKeyGenerator>) alternateKeyGen);
            }
            return keyGenerator;
        }
    }

    /**
     * The value wrapper.
     */
    private class ValueWrapper {
        Object value;
        boolean optional;
    }
}
