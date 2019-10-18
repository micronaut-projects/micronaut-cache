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
package io.micronaut.cache.ehcache.configuration;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.ReadableBytes;
import io.micronaut.core.naming.Named;
import org.ehcache.clustered.client.config.builders.ClusteredResourcePoolBuilder;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.impl.config.store.disk.OffHeapDiskStoreConfiguration;

import javax.annotation.Nonnull;
import java.io.Serializable;

import static io.micronaut.cache.ehcache.configuration.EhcacheConfiguration.PREFIX;

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
    public static final Long DEFAULT_MAX_ENTRIES = 100L;

    @ConfigurationBuilder(prefixes = "with")
    CacheConfigurationBuilder<?, ?> builder;

    private final String name;

    private Class<?> keyType = DEFAULT_KEY_TYPE;
    private Class<?> valueType = DEFAULT_VALUE_TYPE;

    private HeapTieredCacheConfiguration heap;
    private OffheapTieredCacheConfiguration offheap;
    private DiskTieredCacheConfiguration disk;
    private ClusteredDedicatedResourcePoolConfiguration clusteredDedicated;
    private ClusteredSharedResourcePoolConfiguration clusteredShared;

    /**
     * @param name the cache name
     */
    public EhcacheConfiguration(@Parameter String name) {
        this.name = name;
    }

    /**
     * @return the configuration builder
     */
    @SuppressWarnings("unchecked")
    public CacheConfigurationBuilder<?, ?> getBuilder() {
        ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();

        // Resource pools
        boolean resourcePoolAdded = false;

        if (this.heap != null) {
            if (this.heap.getMaxSize() != null) {
                resourcePoolsBuilder = resourcePoolsBuilder.heap(this.heap.getMaxSize(), MemoryUnit.B);
                resourcePoolAdded = true;
            } else if (this.heap.getMaxEntries() != null) {
                resourcePoolsBuilder = resourcePoolsBuilder.heap(this.heap.getMaxEntries(), EntryUnit.ENTRIES);
                resourcePoolAdded = true;
            }
        }

        if (this.offheap != null) {
            if (this.offheap.getMaxSize() != null) {
                resourcePoolsBuilder = resourcePoolsBuilder.offheap(this.offheap.getMaxSize(), MemoryUnit.B);
                resourcePoolAdded = true;
            }
        }

        if (this.disk != null) {
            if (this.disk.getMaxSize() != null) {
                resourcePoolsBuilder = resourcePoolsBuilder.disk(this.disk.getMaxSize(), MemoryUnit.B);
                resourcePoolAdded = true;
            }
        }

        if (this.clusteredDedicated != null) {
            if (this.clusteredDedicated.getMaxSize() != null) {
                if (this.clusteredDedicated.getServerResource() != null) {
                    resourcePoolsBuilder = resourcePoolsBuilder.with(ClusteredResourcePoolBuilder.clusteredDedicated(this.clusteredDedicated.getServerResource(), this.clusteredDedicated.getMaxSize(), MemoryUnit.B));
                    resourcePoolAdded = true;
                } else {
                    resourcePoolsBuilder = resourcePoolsBuilder.with(ClusteredResourcePoolBuilder.clusteredDedicated(this.clusteredDedicated.getMaxSize(), MemoryUnit.B));
                    resourcePoolAdded = true;
                }
            }
        }

        if (this.clusteredShared != null && this.clusteredShared.getServerResource() != null) {
            resourcePoolsBuilder = resourcePoolsBuilder.with(ClusteredResourcePoolBuilder.clusteredShared(this.clusteredShared.getServerResource()));
            resourcePoolAdded = true;
        }

        if (!resourcePoolAdded) {
            resourcePoolsBuilder = ResourcePoolsBuilder.heap(DEFAULT_MAX_ENTRIES);
        }

        this.builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType, resourcePoolsBuilder);

        // Cache configuration

        if (this.heap != null && this.heap.getSizeOfMaxObjectSize() != null) {
            this.builder = this.builder.withSizeOfMaxObjectGraph(this.heap.getSizeOfMaxObjectSize());
        }

        if (this.disk != null && this.disk.getSegments() != null) {
            this.builder = this.builder.withService(new OffHeapDiskStoreConfiguration(this.disk.getSegments()));
        }

        return this.builder;
    }

    /**
     * @param builder The cache configuration builder
     */
    public void setBuilder(CacheConfigurationBuilder builder) {
        this.builder = builder;
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

    /**
     * @return the heap tier configuration
     */
    public HeapTieredCacheConfiguration getHeap() {
        return heap;
    }

    /**
     * @param heap the heap tier configuration
     */
    public void setHeap(HeapTieredCacheConfiguration heap) {
        this.heap = heap;
    }

    /**
     * @return the off-heap configuration
     */
    public OffheapTieredCacheConfiguration getOffheap() {
        return offheap;
    }

    /**
     * @param offheap the off-heap configuration
     */
    public void setOffheap(OffheapTieredCacheConfiguration offheap) {
        this.offheap = offheap;
    }

    /**
     * @return the disk tier configuration
     */
    public DiskTieredCacheConfiguration getDisk() {
        return disk;
    }

    /**
     * @param disk the disk tier configuration
     */
    public void setDisk(DiskTieredCacheConfiguration disk) {
        this.disk = disk;
    }

    /**
     * @return the clustered dedicated configuration
     */
    public ClusteredDedicatedResourcePoolConfiguration getClusteredDedicated() {
        return clusteredDedicated;
    }

    /**
     * @param clusteredDedicated the clustered dedicated configuration
     */
    public void setClusteredDedicated(ClusteredDedicatedResourcePoolConfiguration clusteredDedicated) {
        this.clusteredDedicated = clusteredDedicated;
    }

    /**
     * @return the clustered shared configuration
     */
    public ClusteredSharedResourcePoolConfiguration getClusteredShared() {
        return clusteredShared;
    }

    /**
     * @param clusteredShared the clustered shared configuration
     */
    public void setClusteredShared(ClusteredSharedResourcePoolConfiguration clusteredShared) {
        this.clusteredShared = clusteredShared;
    }

    /**
     * Heap tier configuration properties.
     */
    @ConfigurationProperties(HeapTieredCacheConfiguration.PREFIX)
    public static class HeapTieredCacheConfiguration extends AbstractResourcePoolConfiguration {
        public static final String PREFIX = "heap";

        private Long maxEntries = DEFAULT_MAX_ENTRIES;
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

    /**
     * Off-heap configuration options.
     */
    @ConfigurationProperties(OffheapTieredCacheConfiguration.PREFIX)
    public static class OffheapTieredCacheConfiguration extends AbstractResourcePoolConfiguration {
        public static final String PREFIX = "offheap";
    }

    /**
     * Disk tier configuration options.
     */
    @ConfigurationProperties(DiskTieredCacheConfiguration.PREFIX)
    public static class DiskTieredCacheConfiguration extends AbstractResourcePoolConfiguration {
        public static final String PREFIX = "disk";

        private Integer segments;

        /**
         * @return The disk segments used
         */
        public Integer getSegments() {
            return segments;
        }

        /**
         * @param segments The disk segments used
         */
        public void setSegments(Integer segments) {
            this.segments = segments;
        }
    }

    /**
     * Clustered dedicated configuration.
     */
    @ConfigurationProperties(ClusteredDedicatedResourcePoolConfiguration.PREFIX)
    public static class ClusteredDedicatedResourcePoolConfiguration extends AbstractResourcePoolConfiguration {
        public static final String PREFIX = "clustered-dedicated";

        private String serverResource;

        /**
         * @return the server resource
         */
        public String getServerResource() {
            return serverResource;
        }

        /**
         * @param serverResource the server resource
         */
        public void setServerResource(String serverResource) {
            this.serverResource = serverResource;
        }

    }

    /**
     * Clustered shared configuration.
     */
    @ConfigurationProperties(ClusteredSharedResourcePoolConfiguration.PREFIX)
    public static class ClusteredSharedResourcePoolConfiguration {
        public static final String PREFIX = "clustered-shared";

        private String serverResource;

        /**
         * @return the server resource
         */
        public String getServerResource() {
            return serverResource;
        }

        /**
         * @param serverResource the server resource
         */
        public void setServerResource(String serverResource) {
            this.serverResource = serverResource;
        }

    }
}
