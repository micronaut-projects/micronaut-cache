package io.micronaut.cache.infinispan;

import io.micronaut.cache.CacheInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.ClientStatistics;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class InfinispanCacheInfo implements CacheInfo {

    private final RemoteCache<Object, Object> nativeCache;

    public InfinispanCacheInfo(RemoteCache<Object, Object> nativeCache) {
        this.nativeCache = nativeCache;
    }

    @Nonnull
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

    @Nonnull
    @Override
    public String getName() {
        return nativeCache.getName();
    }
}
