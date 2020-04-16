/*
 * Copyright 2017-2020 original authors
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
import io.micronaut.core.convert.format.ReadableBytes;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.clustered.client.config.builders.ServerSideConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.net.URI;
import java.util.List;

import static io.micronaut.cache.ehcache.configuration.EhcacheCacheManagerConfiguration.PREFIX;

/**
 * Configuration class for the Ehcache {@link org.ehcache.CacheManager}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @see org.ehcache.config.builders.CacheManagerBuilder
 * @since 1.0.0
 */
@ConfigurationProperties(PREFIX)
public class EhcacheCacheManagerConfiguration {

    public static final String PREFIX = "ehcache";

    @ConfigurationBuilder(prefixes = "with", excludes = "")
    private CacheManagerBuilder builder;

    private EhcacheClusterConfiguration cluster;

    private Long defaultSizeOfMaxObjectSize;
    private String storagePath;

    private List<EhcacheClusterResourcePoolConfiguration> resourcePoolConfigurations;

    /**
     * @param resourcePoolConfigurations the resource pool configurations
     */
    public EhcacheCacheManagerConfiguration(List<EhcacheClusterResourcePoolConfiguration> resourcePoolConfigurations) {
        this.resourcePoolConfigurations = resourcePoolConfigurations;
    }

    /**
     * @return the configuration builder
     */
    public CacheManagerBuilder getBuilder() {
        if (this.builder == null) {
            this.builder = CacheManagerBuilder.newCacheManagerBuilder();
        }
        if (this.defaultSizeOfMaxObjectSize != null) {
            this.builder = this.builder.withDefaultSizeOfMaxObjectSize(this.defaultSizeOfMaxObjectSize, MemoryUnit.B);
        }
        if (this.getStoragePath() != null) {
            File storagePath = new File(this.getStoragePath());
            this.builder = this.builder.with(CacheManagerBuilder.persistence(storagePath));
        }
        if (this.cluster != null && this.cluster.getUri() != null) {
            URI clusterUri = URI.create(this.cluster.getUri());
            this.builder = this.builder.with(ClusteringServiceConfigurationBuilder.cluster(clusterUri).autoCreate(this::configureServer));
        }
        return builder;
    }

    /**
     * @param builder the configuration builder
     */
    public void setBuilder(CacheManagerBuilder builder) {
        this.builder = builder;
    }

    /**
     * @return the default maximum size of the largest object in the cache
     */
    public Long getDefaultSizeOfMaxObjectSize() {
        return defaultSizeOfMaxObjectSize;
    }

    /**
     * @param defaultSizeOfMaxObjectSize the default maximum size of the largest object in the cache
     */
    public void setDefaultSizeOfMaxObjectSize(@ReadableBytes Long defaultSizeOfMaxObjectSize) {
        this.defaultSizeOfMaxObjectSize = defaultSizeOfMaxObjectSize;
    }

    /**
     * @return the storage path in the file system
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * @param storagePath the storage path in the file system
     */
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * @return the cluster configuration
     */
    public EhcacheClusterConfiguration getCluster() {
        return cluster;
    }

    /**
     * @param cluster the cluster configuration
     */
    public void setCluster(EhcacheClusterConfiguration cluster) {
        this.cluster = cluster;
    }

    private ServerSideConfigurationBuilder configureServer(ServerSideConfigurationBuilder server) {
        if (this.cluster.getDefaultServerResource() != null) {
            server = server.defaultServerResource(this.cluster.defaultServerResource);
        }
        if (this.resourcePoolConfigurations != null) {
            for (EhcacheClusterResourcePoolConfiguration resourcePoolConfiguration : this.resourcePoolConfigurations) {
                if (resourcePoolConfiguration.getServerResource() != null) {
                    server = server.resourcePool(resourcePoolConfiguration.getName(), resourcePoolConfiguration.getMaxSize(), MemoryUnit.B, resourcePoolConfiguration.getServerResource());
                } else {
                    server = server.resourcePool(resourcePoolConfiguration.getName(), resourcePoolConfiguration.getMaxSize(), MemoryUnit.B);
                }
            }
        }
        return server;
    }

    /**
     * Clustering configuration.
     */
    @ConfigurationProperties(EhcacheClusterConfiguration.PREFIX)
    public static class EhcacheClusterConfiguration {
        public static final String PREFIX = "cluster";

        private String uri;
        private String defaultServerResource;

        /**
         * @return cluster URI
         */
        public String getUri() {
            return uri;
        }

        /**
         * @param uri cluster URI
         */
        public void setUri(String uri) {
            this.uri = uri;
        }

        /**
         * @return the default server resource
         */
        public String getDefaultServerResource() {
            return defaultServerResource;
        }

        /**
         * @param defaultServerResource the default server resource
         */
        public void setDefaultServerResource(String defaultServerResource) {
            this.defaultServerResource = defaultServerResource;
        }
    }
}
