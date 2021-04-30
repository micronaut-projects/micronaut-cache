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
package io.micronaut.cache.noop;

import io.micronaut.cache.SyncCache;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A no operation {@link SyncCache} implementation suitable for disabling caching.
 *
 * @author Marcel Overdijk
 * @since 1.0.0
 */
public class NoOpSyncCache implements SyncCache<Object> {

    private String name;

    /**
     * Constructor.
     *
     * @param name the cache name
     */
    public NoOpSyncCache(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public <T> Optional<T> get(@NonNull Object key, @NonNull Argument<T> requiredType) {
        return Optional.empty();
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        return supplier.get();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T> Optional<T> putIfAbsent(@NonNull Object key, @NonNull T value) {
        return Optional.of(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(@NonNull Object key, @NonNull Object value) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invalidate(@NonNull Object key) {
    }

    @Override
    public void invalidateAll() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }
}
