package io.micronaut.cache.infinispan;

import io.micronaut.cache.DynamicCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.convert.ConversionService;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.BasicConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
public class InfinispanCacheManager implements DynamicCacheManager<RemoteCache<Object, Object>> {

    private final RemoteCacheManager remoteCacheManager;
    private final ConversionService<?> conversionService;

    public InfinispanCacheManager(RemoteCacheManager remoteCacheManager, ConversionService<?> conversionService) {
        this.remoteCacheManager = remoteCacheManager;
        this.conversionService = conversionService;
    }

    @Nonnull
    @Override
    public SyncCache<RemoteCache<Object, Object>> getCache(String name) {
        BasicConfiguration basicConfiguration = new ConfigurationBuilder().build();
        RemoteCache<Object, Object> nativeCache = remoteCacheManager.administration().getOrCreateCache(name, basicConfiguration);
        return new InfinispanSyncCache(nativeCache, conversionService);
    }
}
