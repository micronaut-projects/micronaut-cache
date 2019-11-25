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
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.monitor.LocalMapStats;
import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheInfo;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A {@link SyncCache} implementation based on Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
public class HazelcastSyncCache implements SyncCache<IMap<Object, Object>> {

    private final ConversionService<?> conversionService;
    private final IMap<Object, Object> nativeCache;
    private final ExecutorService executorService;

    /**
     * @param conversionService the conversion service
     * @param nativeCache the native cache
     * @param executorService managers the pool of executors
     */
    public HazelcastSyncCache(ConversionService<?> conversionService,
                              IMap<Object, Object> nativeCache,
                              ExecutorService executorService) {
        this.conversionService = conversionService;
        this.nativeCache = nativeCache;
        this.executorService = executorService;
    }

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
        return get(key, requiredType).orElseGet(supplier);
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
        return nativeCache.getName();
    }

    @Override
    public IMap getNativeCache() {
        return nativeCache;
    }

    @Override
    public AsyncCache<IMap<Object, Object>> async() {
        return new HazelcastAsyncCache(conversionService, nativeCache, executorService);
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        CacheInfo cacheInfo = new CacheInfo() {
            @Nonnull
            @Override
            public Map<String, Object> get() {
                Map<String, Object> data = new LinkedHashMap<>(2);
                data.put("implementationClass", getNativeCache().getClass().getName());
                data.put("hazelcast", getHazelcastInfo());
                return data;
            }

            @Nonnull
            @Override
            public String getName() {
                return this.getName();
            }
        };

        return Publishers.just(cacheInfo);
    }

    private Map<String, Object> getHazelcastInfo() {
//        Map<String, Object> values = new LinkedHashMap<>(8);
        LocalMapStats stats = nativeCache.getLocalMapStats();
        Iterable<JsonObject.Member> iterable = () -> stats.toJson().iterator();
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(member -> new AbstractMap.SimpleEntry<>(member.getName(), member.getValue().asLong()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
//
//
//        values.put("backupCount", stats.getBackupCount());
//        values.put("backupEntryCount", stats.getBackupEntryCount());
//        values.put("backupEntryMemoryCost", stats.getBackupEntryMemoryCost());
//        values.put("creationTime", stats.getCreationTime());
//        values.put("dirtyEntryCount", stats.getDirtyEntryCount());
//        values.put("eventOperationCount", stats.getEventOperationCount());
//        values.put("operationCount", stats.getGetOperationCount());
//        values.put("heapCost", stats.getHeapCost());
//        return values;
    }
}
