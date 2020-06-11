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
package io.micronaut.cache.ignite;

import io.micronaut.cache.AsyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class IgniteAsyncCache implements AsyncCache<IgniteCache> {

    private final ConversionService<?> conversionService;
    private final IgniteCache nativeCache;
    private final ExecutorService executorService;

    public IgniteAsyncCache(ConversionService<?> conversionService, IgniteCache nativeCache,
                            ExecutorService executorService) {
        this.conversionService = conversionService;
        this.nativeCache = nativeCache;
        this.executorService = executorService;
    }

    @Override
    public <T> CompletableFuture<Optional<T>> get(Object key, Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> newFuture = new CompletableFuture<>();
        IgniteFuture<Object> igniteFuture = nativeCache.getAsync(key);
        igniteFuture.listenAsync((IgniteInClosure<IgniteFuture<Object>>) response -> {
            try {
                Object result = response.get();
                newFuture.complete(conversionService.convert(result, ConversionContext.of(requiredType)));
            } catch (IgniteException ex) {
                newFuture.completeExceptionally(ex);
            }
        }, executorService);
        return newFuture;
    }

    @Override
    public <T> CompletableFuture<T> get(Object key, Argument<T> requiredType, Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> optionalCompletableFuture = get(key, requiredType);
        return optionalCompletableFuture.thenApply(existingValue -> {
            if (existingValue.isPresent()) {
                return existingValue.get();
            } else {
                T value = supplier.get();
                put(key, value);
                return value;
            }
        });
    }

    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(Object key, T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        final Class<T> aClass = (Class<T>) value.getClass();
        CompletableFuture<Optional<T>> result = get(key, aClass);

        CompletableFuture<Optional<T>> newFuture = new CompletableFuture<>();
        result.whenComplete((t, throwable) -> {
            try {
                nativeCache.putIfAbsent(key, value);
            } catch (Exception ex) {
                newFuture.completeExceptionally(ex);
            }
            if (throwable != null) {
                newFuture.completeExceptionally(throwable);
            }
            newFuture.complete(t);
        });
        return newFuture;
    }

    @Override
    public CompletableFuture<Boolean> put(Object key, Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        IgniteFuture<Void> igniteFuture = nativeCache.putAsync(key, value);
        igniteFuture.listenAsync((IgniteInClosure<IgniteFuture<Void>>) response -> {
            try {
                response.get();
                future.complete(true);
            } catch (IgniteException ex) {
                future.completeExceptionally(ex);
            }
        }, executorService);
        return future;
    }

    @Override
    public CompletableFuture<Boolean> invalidate(Object key) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        IgniteFuture<Boolean> igniteFuture = nativeCache.removeAsync(key);
        igniteFuture.listen((IgniteInClosure<IgniteFuture<Boolean>>) response -> {
            try {
                future.complete(response.get());
            } catch (IgniteException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        IgniteFuture<Void> igniteFuture = nativeCache.clearAsync();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        igniteFuture.listenAsync((IgniteInClosure<IgniteFuture<Void>>) response -> {
            try {
                response.get();
                future.complete(true);
            } catch (IgniteException ex) {
                future.completeExceptionally(ex);
            }
        }, executorService);
        return future;
    }

    @Override
    public String getName() {
        return nativeCache.getName();
    }

    @Override
    public IgniteCache getNativeCache() {
        return nativeCache;
    }
}
