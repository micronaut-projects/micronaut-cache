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

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.ReadableBytes;
import io.micronaut.core.naming.Named;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import javax.annotation.Nonnull;
import java.io.Serializable;

import static io.micronaut.cache.ehcache.EhcacheConfiguration.PREFIX;

/**
 * Configuration class for an Ehacahe-based cache.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see org.ehcache.config.builders.CacheConfigurationBuilder
 */
@EachProperty(PREFIX)
public class EhcacheConfiguration implements Named {

    public static final String PREFIX = EhcacheCacheManagerConfiguration.PREFIX + ".caches";
    public static final Class<?> DEFAULT_KEY_TYPE = Serializable.class;
    public static final Class<?> DEFAULT_VALUE_TYPE = Serializable.class;
    public static final Long DEFAULT_MAX_ENTRIES = 10L;

    @ConfigurationBuilder(prefixes = "with")
    CacheConfigurationBuilder builder;

    private final String name;

    private Class<?> keyType = DEFAULT_KEY_TYPE;
    private Class<?> valueType = DEFAULT_VALUE_TYPE;

    private HeapTieredCacheConfiguration heap;

    /**
     * @param name the cache name
     */
    public EhcacheConfiguration(@Parameter String name) {
        this.name = name;
    }

    public CacheConfigurationBuilder getBuilder() {
        ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.heap(DEFAULT_MAX_ENTRIES);
        if (this.heap != null) {
            if (this.heap.getMaxSize() != null) {
                resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder().heap(this.heap.getMaxSize(), MemoryUnit.B);
            } else if (this.heap.getMaxEntries() != null) {
                resourcePoolsBuilder = ResourcePoolsBuilder.heap(this.heap.getMaxEntries());
            }
        }
        this.builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType, resourcePoolsBuilder);

        if (this.heap != null && this.heap.getSizeOfMaxObjectSize() != null) {
            this.builder.withSizeOfMaxObjectGraph(this.heap.getSizeOfMaxObjectSize());
        }

        return this.builder;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return The type of the keys in the cache
     */
    public Class<?> getKeyType() {
        return keyType;
    }

    /**
     * @param keyType The type of the keys in the cache
     */
    public void setKeyType(Class<?> keyType) {
        this.keyType = keyType;
    }

    /**
     * @return The type of the values in the cache
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * @param valueType The type of the values in the cache
     */
    public void setValueType(Class<?> valueType) {
        this.valueType = valueType;
    }

    public HeapTieredCacheConfiguration getHeap() {
        return heap;
    }

    public void setHeap(HeapTieredCacheConfiguration heap) {
        this.heap = heap;
    }

    @ConfigurationProperties(HeapTieredCacheConfiguration.PREFIX)
    public static class HeapTieredCacheConfiguration {
        public static final String PREFIX = "heap";

        private Long maxEntries = DEFAULT_MAX_ENTRIES;
        private Long maxSize;
        private Long sizeOfMaxObjectSize;

        /**
         * @return The maximum number of entries
         */
        public Long getMaxEntries() {
            return maxEntries;
        }

        /**
         * @param maxEntries The maximum number of entries
         */
        public void setMaxEntries(Long maxEntries) {
            this.maxEntries = maxEntries;
        }

        /**
         * @return The maximum size of the cache, in bytes
         */
        public Long getMaxSize() {
            return maxSize;
        }

        /**
         * @param maxSize The maximum size of the cache, in bytes
         */
        public void setMaxSize(@ReadableBytes Long maxSize) {
            this.maxSize = maxSize;
        }

        /**
         * @return The maximum size of a single object
         */
        public Long getSizeOfMaxObjectSize() {
            return sizeOfMaxObjectSize;
        }

        /**
         * @param sizeOfMaxObjectSize The maximum size of a single object
         */
        public void setSizeOfMaxObjectSize(@ReadableBytes Long sizeOfMaxObjectSize) {
            this.sizeOfMaxObjectSize = sizeOfMaxObjectSize;
        }
    }
}
