/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.infinispan;

import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheInfo;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An {@link AsyncCache} implementation based on Infinispan's {@link org.infinispan.commons.api.AsyncCache}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class InfinispanAsyncCache implements AsyncCache<RemoteCache<Object, Object>> {

    private final RemoteCache<Object, Object> nativeCache;
    private final ConversionService<?> conversionService;

    /**
     * @param nativeCache the Infinispan remote cache
     * @param conversionService the conversion service
     */
    public InfinispanAsyncCache(RemoteCache<Object, Object> nativeCache, ConversionService<?> conversionService) {
        this.nativeCache = nativeCache;
        this.conversionService = conversionService;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        return nativeCache.getAsync(key).thenApply(value -> conversionService.convert(value, ConversionContext.of(requiredType)));
    }

    @Override
    public <T> CompletableFuture<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
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

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        return nativeCache.putIfAbsentAsync(key, value).thenApply(val -> {
            final T v = (T) val;
            final Class<T> aClass = (Class<T>) value.getClass();
            return conversionService.convert(v, aClass);
        });
    }

    @Override
    public CompletableFuture<Boolean> put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        return nativeCache
                .putAsync(key, value)
                .thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        return nativeCache
                .removeAsync(key)
                .thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        return nativeCache
                .clearAsync()
                .handle((aVoid, throwable) -> throwable != null);
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        return Publishers.just(new InfinispanCacheInfo(nativeCache));
    }

    @Override
    public String getName() {
        return nativeCache.getName();
    }

    @Override
    public RemoteCache<Object, Object> getNativeCache() {
        return nativeCache;
    }
}
