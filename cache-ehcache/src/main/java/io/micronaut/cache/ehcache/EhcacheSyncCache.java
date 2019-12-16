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

import io.micronaut.cache.CacheInfo;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.ehcache.configuration.EhcacheConfiguration;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import io.micronaut.scheduling.TaskExecutors;
import org.ehcache.Cache;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A {@link SyncCache} implementation based on Ehcache.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class EhcacheSyncCache implements SyncCache<Cache> {

    private final ConversionService<?> conversionService;
    private final EhcacheConfiguration configuration;
    private final Cache nativeCache;
    private final ExecutorService executorService;
    private final StatisticsService statisticsService;

    /**
     * @param conversionService the conversion service
     * @param configuration the configuration
     * @param nativeCache the native cache
     * @param executorService the executor service to offload synchronous operations
     * @param statisticsService th Ehcache statistics service
     */
    public EhcacheSyncCache(ConversionService<?> conversionService,
                            EhcacheConfiguration configuration,
                            Cache nativeCache,
                            @Named(TaskExecutors.IO) ExecutorService executorService,
                            StatisticsService statisticsService) {
        this.conversionService = conversionService;
        this.configuration = configuration;
        this.nativeCache = nativeCache;
        this.executorService = executorService;
        this.statisticsService = statisticsService;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> Optional<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        Object value = nativeCache.get(key);
        if (value != null) {
            return conversionService.convert(value, ConversionContext.of(requiredType));
        }
        return Optional.empty();
    }

    @Override
    public <T> T get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        Optional<T> existingValue = get(key, requiredType);
        if (existingValue.isPresent()) {
            return existingValue.get();
        } else {
            T value = supplier.get();
            put(key, value);
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> Optional<T> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        final T v = (T) nativeCache.putIfAbsent(key, value);
        final Class<T> aClass = (Class<T>) value.getClass();
        return conversionService.convert(v, aClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        nativeCache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        nativeCache.remove(key);
    }

    @Override
    public void invalidateAll() {
        nativeCache.clear();
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public Cache getNativeCache() {
        return nativeCache;
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        CacheInfo cacheInfo = new CacheInfo() {
            @Nonnull
            @Override
            public Map<String, Object> get() {
                Map<String, Object> data = new LinkedHashMap<>(2);
                data.put("implementationClass", getNativeCache().getClass().getName());
                data.put("ehcache", getEhcacheInfo());
                return data;
            }

            @Nonnull
            @Override
            public String getName() {
                return configuration.getName();
            }
        };

        return Publishers.just(cacheInfo);
    }

    private Map<String, Object> getEhcacheInfo() {
        Map<String, Object> values = new LinkedHashMap<>(8);
        CacheStatistics statistics = statisticsService.getCacheStatistics(configuration.getName());
        values.put("cacheEvictions", statistics.getCacheEvictions());
        values.put("cacheExpirations", statistics.getCacheExpirations());
        values.put("cacheGets", statistics.getCacheGets());
        values.put("cacheHitPercentage", statistics.getCacheHitPercentage());
        values.put("cacheHits", statistics.getCacheHits());
        values.put("cacheMisses", statistics.getCacheMisses());
        values.put("cacheMissPercentage", statistics.getCacheMissPercentage());
        values.put("cachePuts", statistics.getCachePuts());
        values.put("cacheRemovals", statistics.getCacheRemovals());
        return values;
    }
}
