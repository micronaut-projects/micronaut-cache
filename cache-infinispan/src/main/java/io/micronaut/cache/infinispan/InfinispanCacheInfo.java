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
package io.micronaut.cache.infinispan;

import io.micronaut.cache.CacheInfo;
import io.micronaut.core.annotation.NonNull;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides Infinispan cache statistics.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see RemoteCacheClientStatisticsMXBean
 * @see org.infinispan.client.hotrod.ServerStatistics
 */
public class InfinispanCacheInfo implements CacheInfo {

    private final RemoteCache<Object, Object> nativeCache;

    /**
     * @param nativeCache the Infinispan remote cache
     */
    public InfinispanCacheInfo(RemoteCache<Object, Object> nativeCache) {
        this.nativeCache = nativeCache;
    }

    @NonNull
    @Override
    public Map<String, Object> get() {
        Map<String, Object> cacheManager = getCacheManagerStatistics();
        Map<String, Object> clientStatistics = getClientStatistics();
        Map<String, String> serverStatistics = nativeCache.serverStatistics().getStatsMap();

        Map<String, Object> infinispan = new LinkedHashMap<>(3);
        infinispan.put("cacheManager", cacheManager);
        infinispan.put("clientStatistics", clientStatistics);
        infinispan.put("serverStatistics", serverStatistics);

        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("implementationClass", nativeCache.getClass().getName());
        data.put("infinispan", infinispan);
        return data;
    }

    private Map<String, Object> getCacheManagerStatistics() {
        Map<String, Object> cacheManager = new LinkedHashMap<>(3);
        RemoteCacheManager remoteCacheManager = nativeCache.getRemoteCacheManager();
        cacheManager.put("activeConnectionCount", remoteCacheManager.getActiveConnectionCount());
        cacheManager.put("connectionCount", remoteCacheManager.getConnectionCount());
        cacheManager.put("idleConnectionCount", remoteCacheManager.getIdleConnectionCount());
        return cacheManager;
    }

    private Map<String, Object> getClientStatistics() {
        Map<String, Object> clientStatistics = new LinkedHashMap<>(12);
        RemoteCacheClientStatisticsMXBean clientStats = nativeCache.clientStatistics();
        clientStatistics.put("averageRemoteReadTime", clientStats.getAverageRemoteReadTime());
        clientStatistics.put("averageRemoteRemovesTime", clientStats.getAverageRemoteRemovesTime());
        clientStatistics.put("averageRemoteStoreTime", clientStats.getAverageRemoteStoreTime());
        clientStatistics.put("nearCacheHits", clientStats.getNearCacheHits());
        clientStatistics.put("nearCacheInvalidations", clientStats.getNearCacheInvalidations());
        clientStatistics.put("nearCacheMisses", clientStats.getNearCacheMisses());
        clientStatistics.put("nearCacheSize", clientStats.getNearCacheSize());
        clientStatistics.put("remoteHits", clientStats.getRemoteHits());
        clientStatistics.put("remoteMisses", clientStats.getRemoteMisses());
        clientStatistics.put("remoteRemoves", clientStats.getRemoteRemoves());
        clientStatistics.put("remoteStores", clientStats.getRemoteStores());
        clientStatistics.put("timeSinceReset", clientStats.getTimeSinceReset());
        return clientStatistics;
    }

    @NonNull
    @Override
    public String getName() {
        return nativeCache.getName();
    }
}
