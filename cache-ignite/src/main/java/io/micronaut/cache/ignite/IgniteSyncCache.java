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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.apache.ignite.IgniteCache;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class IgniteSyncCache implements SyncCache<IgniteCache> {
    private final ConversionService<?> conversionService;
    private final IgniteCache nativeCache;
    private final ExecutorService executorService;

    public IgniteSyncCache(ConversionService<?> conversionService, IgniteCache nativeCache, ExecutorService executorService) {
        this.conversionService = conversionService;
        this.nativeCache = nativeCache;
        this.executorService = executorService;
    }

    @NonNull
    @Override
    public AsyncCache<IgniteCache> async() {
        return new IgniteAsyncCache(conversionService, nativeCache, executorService);
    }

    @NonNull
    @Override
    public <T> Optional<T> get(@NonNull Object key, @NonNull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        Object value = nativeCache.get(key);
        if (value != null) {
            return conversionService.convert(value, ConversionContext.of(requiredType));
        }
        return Optional.empty();
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        Optional<T> existingValue = get(key, requiredType);
        if (existingValue.isPresent()) {
            return existingValue.get();
        } else {
            T value = supplier.get();
            put(key, value);
            return value;
        }
    }

    @NonNull
    @Override
    public <T> Optional<T> putIfAbsent(@NonNull Object key, @NonNull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        final Class<T> aClass = (Class<T>) value.getClass();
        Optional<T> lastResult = get(key, aClass);
        nativeCache.putIfAbsent(key, value);
        return lastResult;
    }

    @Override
    public void put(@NonNull Object key, @NonNull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        nativeCache.putIfAbsent(key, value);
    }

    @Override
    public void invalidate(@NonNull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        nativeCache.remove(key);

    }

    @Override
    public void invalidateAll() {
        nativeCache.clear();
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
