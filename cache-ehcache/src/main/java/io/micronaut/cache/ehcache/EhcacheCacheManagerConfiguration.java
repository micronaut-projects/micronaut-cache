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
import io.micronaut.core.convert.format.ReadableBytes;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.units.MemoryUnit;

import static io.micronaut.cache.ehcache.EhcacheCacheManagerConfiguration.PREFIX;

/**
 * Configuration class for the Ehcache {@link org.ehcache.CacheManager}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see org.ehcache.config.builders.CacheManagerBuilder
 */
@ConfigurationProperties(PREFIX)
public class EhcacheCacheManagerConfiguration {

    public static final String PREFIX = "ehcache";

    @ConfigurationBuilder(prefixes = "with", excludes = "")
    private CacheManagerBuilder builder;

    private Long defaultSizeOfMaxObjectSize;

    /**
     * @return the configuration builder
     */
    public CacheManagerBuilder getBuilder() {
        if (this.builder == null) {
            this.builder = CacheManagerBuilder.newCacheManagerBuilder();
        }
        if (this.defaultSizeOfMaxObjectSize != null) {
            this.builder.withDefaultSizeOfMaxObjectSize(this.defaultSizeOfMaxObjectSize, MemoryUnit.B);
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
}
