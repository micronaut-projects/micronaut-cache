package io.micronaut.cache.infinispan;

import io.micronaut.cache.CacheInfo;
import org.infinispan.client.hotrod.RemoteCache;

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
        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("implementationClass", nativeCache.getClass().getName());
        data.put("infinispan", nativeCache.clientStatistics().getAverageRemoteReadTime());
        return data;
    }

    @Nonnull
    @Override
    public String getName() {
        return nativeCache.getName();
    }
}
