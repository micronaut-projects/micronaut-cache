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
package io.micronaut.cache.hazelcast;

import com.hazelcast.map.IMap;
import io.micronaut.cache.AbstractMapBasedSyncCache;
import io.micronaut.cache.AsyncCache;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.ArgumentUtils;

import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.cache.SyncCache} implementation based on Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
public class HazelcastSyncCache extends AbstractMapBasedSyncCache<IMap<Object, Object>> {

    private final ExecutorService executorService;

    /**
     * @param conversionService the conversion service
     * @param nativeCache the native cache
     * @param executorService managers the pool of executors
     */
    public HazelcastSyncCache(ConversionService<?> conversionService,
                              IMap<Object, Object> nativeCache,
                              ExecutorService executorService) {
        super(conversionService, nativeCache);
        this.executorService = executorService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(@NonNull Object key, @NonNull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        getNativeCache().set(key, value);
    }

    @Override
    public String getName() {
        return getNativeCache().getName();
    }

    @Override
    public AsyncCache<IMap<Object, Object>> async() {
        return new HazelcastAsyncCache(getConversionService(), getNativeCache(), executorService);
    }
}
