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
package io.micronaut.cache.ehcache;

import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.ehcache.configuration.EhcacheConfiguration;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.scheduling.TaskExecutors;
import org.ehcache.Cache;
import org.ehcache.CacheManager;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * A {@link io.micronaut.cache.CacheManager} implementation for Ehcache.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Replaces(DefaultCacheManager.class)
@Primary
public class EhcacheCacheManager implements io.micronaut.cache.CacheManager<Cache>, Closeable {

    private final CacheManager cacheManager;
    private final ConversionService<?> conversionService;
    private final ExecutorService executorService;
    private Map<String, EhcacheConfiguration> cacheConfigurations;
    private Map<String, EhcacheSyncCache> cacheMap;

    /**
     * @param cacheManager the cache manager
     * @param executorService the executor service
     * @param conversionService the conversion service
     * @param cacheConfigurations the cache configuration
     */
    public EhcacheCacheManager(@Nonnull CacheManager cacheManager,
                               @Nonnull @Named(TaskExecutors.IO) ExecutorService executorService,
                               @Nonnull ConversionService<?> conversionService,
                               @Nonnull List<EhcacheConfiguration> cacheConfigurations) {
        this.cacheManager = cacheManager;
        this.conversionService = conversionService;
        this.executorService = executorService;
        this.cacheConfigurations = cacheConfigurations
                .stream()
                .collect(Collectors.toMap(EhcacheConfiguration::getName, ehcacheConfiguration -> ehcacheConfiguration));
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
            if (configuration != null) {
                Cache nativeCache = this.cacheManager.createCache(name, configuration.getBuilder());
                syncCache = new EhcacheSyncCache(conversionService, configuration, nativeCache, executorService);
                this.cacheMap.put(name, syncCache);
            } else {
                throw new ConfigurationException("No cache configured for name: " + name);
            }
        }
        return syncCache;
    }

    @Override
    public void close() throws IOException {
        this.cacheManager.close();
    }

}
