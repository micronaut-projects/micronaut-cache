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
package io.micronaut.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.ConversionService;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link io.micronaut.cache.CacheManager} implementation for Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@Replaces(DefaultCacheManager.class)
@Primary
public class HazelcastManager implements io.micronaut.cache.CacheManager<IMap>, Closeable {

    private Map<String, HazelcastConfiguration> cacheConfigurations;
    private Map<String, HazelcastSyncCache> cacheMap;
    private final ConversionService<?> conversionService;

    /**
     * @param cacheConfigurations the cache configuration
     */
    public HazelcastManager( @Nonnull ConversionService<?> conversionService,
                             @Nonnull List<HazelcastConfiguration> cacheConfigurations) {
        this.conversionService = conversionService;
        this.cacheConfigurations = cacheConfigurations
                .stream()
                .collect(Collectors.toMap(HazelcastConfiguration::getName, hazelcastConfiguration -> hazelcastConfiguration));
        this.cacheMap = new HashMap<>(cacheConfigurations.size());
    }

    @Nonnull
    @Override
    public Set<String> getCacheNames() {
        return this.cacheConfigurations.keySet();
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public SyncCache<IMap> getCache(String name) {
        HazelcastSyncCache syncCache = this.cacheMap.get(name);
        if (syncCache == null) {
            HazelcastConfiguration configuration = cacheConfigurations.get(name);

            HazelcastInstance instance;
            if (configuration.getUseDefaultHazelcastXml()) {
                instance = Hazelcast.newHazelcastInstance();
            } else {
                Config nativeHazelcastConfig = createNativeHazelCastConfig(configuration);
                instance = Hazelcast.newHazelcastInstance(nativeHazelcastConfig);
            }

            IMap nativeCache = instance.getMap(configuration.getName());
            syncCache = new HazelcastSyncCache(conversionService, configuration, nativeCache);
            this.cacheMap.put(name, syncCache);
        }
        return syncCache;
    }

    @Override
    public void close() throws IOException {
        Hazelcast.shutdownAll();
    }

    private Config createNativeHazelCastConfig(HazelcastConfiguration configuration) {
        Config nativeHazelcastConfig = new Config();
        MapConfig mapConfig = new MapConfig();
        mapConfig.setName(configuration.getName());

        if (configuration.getBackupCount() != null) {
            mapConfig.setBackupCount(configuration.getBackupCount());
        }

        if (configuration.getTimeToLiveSeconds() != null) {
            mapConfig.setTimeToLiveSeconds(configuration.getTimeToLiveSeconds());
        }

        nativeHazelcastConfig.addMapConfig(mapConfig);
        return nativeHazelcastConfig;
    }
}
