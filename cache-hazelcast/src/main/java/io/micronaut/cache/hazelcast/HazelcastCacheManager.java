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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.micronaut.cache.DynamicCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.cache.CacheManager} implementation for Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@Singleton
public class HazelcastCacheManager implements DynamicCacheManager<IMap<Object, Object>> {

    private final ConversionService conversionService;
    private final ExecutorService executorService;
    private final HazelcastInstance hazelcastInstance;

    /**
     * Constructor.
     *
     * @param conversionService convert values that are returned
     * @param hazelcastInstance the client instance of hazelcast client
     * @param executorService managers the pool of executors
     */
    public HazelcastCacheManager(ConversionService conversionService,
                                 HazelcastInstance hazelcastInstance,
                                 @Named(TaskExecutors.IO) ExecutorService executorService) {
        this.conversionService = conversionService;
        this.executorService = executorService;
        this.hazelcastInstance = hazelcastInstance;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public SyncCache<IMap<Object, Object>> getCache(String name) {
        IMap<Object, Object> nativeCache = hazelcastInstance.getMap(name);
        return new HazelcastSyncCache(conversionService, nativeCache, executorService);
    }
}
