package io.micronaut.cache.ehcache;

import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.TaskExecutors;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;

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
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Replaces(DefaultCacheManager.class)
@Primary
public class EhcacheManager implements io.micronaut.cache.CacheManager<Cache>, Closeable {

    private final CacheManager cacheManager;
    private final ConversionService<?> conversionService;
    private final ExecutorService executorService;
    private Map<String, EhcacheConfiguration> cacheConfigurations;
    private Map<String, EhcacheSyncCache> cacheMap;



    public EhcacheManager(@Nonnull CacheManager cacheManager,
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
