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
package io.micronaut.cache.ehcache;

import io.micronaut.cache.ehcache.configuration.EhcacheCacheManagerConfiguration;
import io.micronaut.cache.ehcache.configuration.EhcacheConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.DefaultStatisticsService;

import java.util.concurrent.ExecutorService;

/**
 * Factory class that creates an Ehcache {@link CacheManager}, an {@link EhcacheSyncCache} and an {@link StatisticsService} beans.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Factory
public class EhcacheCacheFactory {

    private EhcacheCacheManagerConfiguration configuration;

    /**
     * @param configuration the configuration
     */
    public EhcacheCacheFactory(EhcacheCacheManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @param statisticsService the Ehcache statistics service
     * @return The {@link CacheManagerBuilder}
     */
    @Singleton
    CacheManagerBuilder<CacheManager> cacheManagerBuilder(StatisticsService statisticsService) {
        return configuration.getBuilder().using(statisticsService);
    }

    /**
     * @param builder The {@link org.ehcache.config.builders.CacheManagerBuilder}
     * @return The {@link CacheManager}
     */
    @Singleton
    @Bean(preDestroy = "close")
    CacheManager cacheManager(CacheManagerBuilder<CacheManager> builder) {
        return builder.build(true);
    }

    /**
     * @return the Ehcache statistics service
     */
    @Singleton
    @Bean(preDestroy = "stop")
    StatisticsService statisticsService() {
        return new DefaultStatisticsService();
    }

    /**
     * Creates a cache instance based on configuration.
     *
     * @param configuration     The configuration
     * @param cacheManager      The cache manager
     * @param conversionService The conversion service
     * @param executorService   The executor
     * @param statisticsService The statistics service
     * @return The sync cache
     */
    @EachBean(EhcacheConfiguration.class)
    EhcacheSyncCache syncCache(@Parameter EhcacheConfiguration configuration,
                               CacheManager cacheManager,
                               ConversionService conversionService,
                               @Named(TaskExecutors.IO) ExecutorService executorService,
                               StatisticsService statisticsService) {
        Cache<?, ?> nativeCache = cacheManager.createCache(configuration.getName(), configuration.getBuilder());
        return new EhcacheSyncCache(conversionService, configuration, nativeCache, executorService, statisticsService);
    }

}
