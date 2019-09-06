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

import com.hazelcast.core.IMap;
import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;

import javax.annotation.Nonnull;
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
public class HazelcastManager implements io.micronaut.cache.CacheManager<IMap> {

    private Map<String, HazelcastConfiguration> cacheConfigurations;
    private Map<String, EhcacheSyncCache> cacheMap;

    /**
     * @param cacheConfigurations the cache configuration
     */
    public HazelcastManager(@Nonnull List<HazelcastConfiguration> cacheConfigurations) {
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
    public SyncCache<Cache> getCache(String name) {
        EhcacheSyncCache syncCache = this.cacheMap.get(name);
        if (syncCache == null) {
            EhcacheConfiguration configuration = cacheConfigurations.get(name);
            Cache nativeCache = this.cacheManager.createCache(name, configuration.builder.build());
            syncCache = new EhcacheSyncCache(conversionService, configuration, nativeCache);
            this.cacheMap.put(name, syncCache);
        }
        return syncCache;
    }

    @Override
    public void close() throws IOException {
        this.cacheManager.close();
    }

}
