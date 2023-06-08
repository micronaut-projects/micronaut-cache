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
package io.micronaut.cache.caffeine;

import com.github.benmanes.caffeine.cache.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micronaut.cache.CacheConfiguration;
import io.micronaut.cache.CacheInfo;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.caffeine.configuration.CaffeineCacheConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>A default {@link SyncCache} implementation based on Caffeine</p>
 * <p>
 * <p>Since Caffeine is a non-blocking in-memory cache the {@link #async()} method will return an implementation that
 * runs operations in the current thread.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@EachBean(CacheConfiguration.class)
public class DefaultSyncCache implements SyncCache<Cache> {

    private final CacheConfiguration cacheConfiguration;
    private final com.github.benmanes.caffeine.cache.Cache cache;
    private final ApplicationContext applicationContext;
    private final ConversionService conversionService;

    /**
     * Construct a sync cache implementation with given configurations.
     *
     * @param cacheConfiguration The cache configurations
     * @param applicationContext The application context
     * @param conversionService To convert the value from the cache into given required type
     */
    public DefaultSyncCache(
            DefaultCacheConfiguration cacheConfiguration,
            ApplicationContext applicationContext,
            ConversionService conversionService) {
        this((CacheConfiguration) cacheConfiguration, applicationContext, conversionService);
    }

    /**
     * Construct a sync cache implementation with given configurations.
     *
     * @param cacheConfiguration The cache configurations
     * @param applicationContext The application context
     * @param conversionService To convert the value from the cache into given required type
     */
    @Inject
    public DefaultSyncCache(
            CacheConfiguration cacheConfiguration,
            ApplicationContext applicationContext,
            ConversionService conversionService) {
        this.cacheConfiguration = cacheConfiguration;
        this.applicationContext = applicationContext;
        this.conversionService = conversionService;
        this.cache = buildCache(cacheConfiguration);
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        return Flux.just(new CacheInfo() {
            @NonNull
            @Override
            public String getName() {
                return cacheConfiguration.getCacheName();
            }

            @NonNull
            @Override
            public Map<String, Object> get() {
                Map<String, Object> data = new LinkedHashMap<>(2);
                data.put("implementationClass", getNativeCache().getClass().getName());
                data.put("caffeine", getCaffeineCacheData(cache));
                return data;
            }
        });
    }

    @Override
    public String getName() {
        return cacheConfiguration.getCacheName();
    }

    @Override
    public com.github.benmanes.caffeine.cache.Cache getNativeCache() {
        return cache;
    }

    @Override
    public <T> Optional<T> get(Object key, Argument<T> requiredType) {
        Object value = cache.getIfPresent(key);
        if (value != null) {
            return conversionService.convert(value, ConversionContext.of(requiredType));
        }
        return Optional.empty();
    }

    @Override
    public <T> T get(Object key, Argument<T> requiredType, Supplier<T> supplier) {
        Object value = cache.get(key, o -> supplier.get());
        if (value != null) {
            Optional<T> converted = conversionService.convert(value, ConversionContext.of(requiredType));
            return converted.orElseThrow(() ->
                new IllegalArgumentException("Cache supplier returned a value that cannot be converted to type: " + requiredType.getName())
            );
        }
        return (T) value;
    }

    @Override
    public void invalidate(Object key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * <p>Cache the specified value using the specified key. If the value is null, it will call
     * {@link #invalidate(Object)} passing the key</p>
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    @SuppressWarnings("unchecked")
    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        if (value == null) {
            // null is the same as removal
            cache.invalidate(key);
        } else {
            cache.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> putIfAbsent(Object key, T value) {
        Object previous = cache.asMap().putIfAbsent(key, value);
        return Optional.ofNullable((T) previous);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T putIfAbsent(Object key, Supplier<T> value) {
        Object val = cache.asMap().computeIfAbsent(key, (k) -> value.get());
        return (T) val;
    }

    /**
     * Build a cache from the given configurations.
     *
     * @param cacheConfiguration The cache configurations
     * @return cache
     */
    protected com.github.benmanes.caffeine.cache.Cache buildCache(CacheConfiguration cacheConfiguration) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        cacheConfiguration.getExpireAfterAccess().ifPresent(duration -> builder.expireAfterAccess(duration.toMillis(), TimeUnit.MILLISECONDS));
        cacheConfiguration.getExpireAfterWrite().ifPresent(duration -> builder.expireAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS));
        cacheConfiguration.getRefreshAfterWrite().ifPresent(duration -> builder.refreshAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS));
        cacheConfiguration.getInitialCapacity().ifPresent(builder::initialCapacity);
        cacheConfiguration.getMaximumSize().ifPresent(builder::maximumSize);
        cacheConfiguration.getMaximumWeight().ifPresent(weight -> {
            builder.maximumWeight(weight);
            builder.weigher(findWeigher());
        });
        CaffeineCacheConfiguration caffeineCacheConfiguration = cacheConfiguration instanceof CaffeineCacheConfiguration ? (CaffeineCacheConfiguration) cacheConfiguration : null;
        if (caffeineCacheConfiguration != null) {
            RemovalListener removalListener = findRemovalListener();
            if (removalListener != null) {
                if (caffeineCacheConfiguration.isListenToRemovals()) {
                    builder.removalListener((key, value, cause) -> removalListener.onRemoval(key, value, cause));
                }
                if (caffeineCacheConfiguration.isListenToEvictions()) {
                    builder.evictionListener((key, value, cause) -> removalListener.onRemoval(key, value, cause));
                }
            }
        }
        if (cacheConfiguration.isRecordStats()) {
            builder.recordStats();
        }
        if (cacheConfiguration.isTestMode()) {
            // run commands on same thread
            builder.executor(Runnable::run);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Weigher<Object, Object> findWeigher() {
        return applicationContext.findBean(Weigher.class, Qualifiers.byName(cacheConfiguration.getCacheName()))
                .orElseGet(() -> applicationContext.findBean(Weigher.class)
                        .orElse(Weigher.singletonWeigher()));
    }

    @SuppressWarnings("unchecked")
    private RemovalListener<Object, Object> findRemovalListener() {
         return applicationContext.findBean(RemovalListener.class, Qualifiers.byName(cacheConfiguration.getCacheName()))
                .orElseGet(() -> applicationContext.findBean(RemovalListener.class)
                        .orElse(null));
    }

    private Map<String, Object> getCaffeineCacheData(Cache caffeineCache) {

        Policy policy = caffeineCache.policy();
        Optional<Policy.Eviction> eviction = policy.eviction();
        Optional<Policy.FixedExpiration> expireAfterAccess = policy.expireAfterAccess();
        Optional<Policy.FixedExpiration> expireAfterWrite = policy.expireAfterWrite();

        Long maximumSize = eviction.filter(e -> !e.isWeighted()).map(e -> e.getMaximum()).orElse(null);
        Long maximumWeight = eviction.filter(e -> e.isWeighted()).map(e -> e.getMaximum()).orElse(null);
        Long weightedSize = eviction.flatMap(e -> e.weightedSize().isPresent() ? Optional.of(e.weightedSize().getAsLong()) : Optional.empty()).orElse(null);
        boolean isRecordingStats = policy.isRecordingStats();

        Map<String, Object> values = new LinkedHashMap<>(8);

        values.put("estimatedSize", caffeineCache.estimatedSize());
        values.put("maximumSize", maximumSize);
        values.put("maximumWeight", maximumWeight);
        values.put("weightedSize", weightedSize);
        values.put("expireAfterAccess", getExpiresAfter(expireAfterAccess));
        values.put("expireAfterWrite", getExpiresAfter(expireAfterWrite));
        values.put("recordingStats", isRecordingStats);

        if (isRecordingStats) {
            values.put("stats", getStatsData(caffeineCache.stats()));
        }

        return values;
    }

    private Long getExpiresAfter(Optional<Policy.FixedExpiration> expiration) {
        return expiration.map(e -> e.getExpiresAfter(TimeUnit.MILLISECONDS)).orElse(null);
    }

    private Map<String, Object> getStatsData(CacheStats stats) {

        Map<String, Object> values = new LinkedHashMap<>(13);

        values.put("requestCount", stats.requestCount());
        values.put("hitCount", stats.hitCount());
        values.put("hitRate", stats.hitRate());
        values.put("missCount", stats.missCount());
        values.put("missRate", stats.missRate());
        values.put("evictionCount", stats.evictionCount());
        values.put("evictionWeight", stats.evictionWeight());

        return values;
    }
}
