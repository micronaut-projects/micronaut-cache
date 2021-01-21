/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.cache.coherence;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.cache.AsyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A {@link io.micronaut.cache.SyncCache} implementation based on Coherence.
 *
 * @author Vaso Putica
 */
public class CoherenceAsyncCache implements AsyncCache<NamedCache<Object, Object>> {

    private final ConversionService<?> conversionService;
    private final AsyncNamedCache<Object, Object> asyncCache;
    private final ExecutorService executorService;

    /**
     * @param conversionService the conversion service
     * @param nativeCache       the native cache
     * @param executorService   managers the pool of executors
     */
    public CoherenceAsyncCache(ConversionService<?> conversionService,
                               NamedCache<Object, Object> nativeCache,
                               ExecutorService executorService) {
        this.conversionService = conversionService;
        this.asyncCache = nativeCache.async();
        this.executorService = executorService;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> get(@NonNull Object key, @NonNull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        asyncCache.get(key).whenComplete((response, throwable) -> {
            if (throwable == null) {
                if (response != null) {
                    future.complete(conversionService.convert(response, ConversionContext.of(requiredType)));
                } else {
                    future.complete(Optional.empty());
                }
            } else {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    @Override
    public <T> CompletableFuture<T> get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        return get(key, requiredType).thenApply(existingValue -> {
            if (existingValue.isPresent()) {
                return existingValue.get();
            } else {
                T value = supplier.get();
                put(key, value);
                return value;
            }
       });
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@NonNull Object key, @NonNull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        return asyncCache.putIfAbsent(key, value).thenApply(existingValue -> {
            if (existingValue == null) {
                return Optional.empty();
            }
            final Class<T> aClass = (Class<T>) value.getClass();
            return conversionService.convert(existingValue, aClass);
        });
    }

    @Override
    public CompletableFuture<Boolean> put(@NonNull Object key, @NonNull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        asyncCache.put(key, value).whenComplete((response, throwable) -> {
            if (throwable == null) {
                future.complete(true);
            } else {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> invalidate(Object key) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        asyncCache.remove(key).whenComplete((response, throwable) -> {
            if (throwable == null) {
                future.complete(true);
            } else {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        asyncCache.clear().thenRun(() -> future.complete(true));
        return future;
    }

    @Override
    public String getName() {
        return asyncCache.getNamedCache().getCacheName();
    }

    @Override
    public NamedCache<Object, Object> getNativeCache() {
        return asyncCache.getNamedCache();
    }
}
