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
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

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
    public static final Integer DEFAULT_MAX_ENTRIES = 10;
    public static final Class<?> DEFAULT_KEY_TYPE = Serializable.class;
    public static final Class<?> DEFAULT_VALUE_TYPE = Serializable.class;

    @ConfigurationBuilder(prefixes = "with")
    CacheConfigurationBuilder builder;

    private final String name;

    private Integer maxEntries = DEFAULT_MAX_ENTRIES;
    private Class<?> keyType = DEFAULT_KEY_TYPE;
    private Class<?> valueType = DEFAULT_VALUE_TYPE;

    /**
     * @param name the cache name
     */
    public EhcacheConfiguration(@Parameter String name) {
        this.name = name;
        initBuilder();
    }

    private void initBuilder() {
        //TODO allow customisations
        this.builder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(keyType, valueType, ResourcePoolsBuilder.heap(maxEntries));
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return The maximum number of entries
     */
    public Integer getMaxEntries() {
        return maxEntries;
    }

    /**
     * @param maxEntries The maximum number of entries
     */
    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
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
        initBuilder();
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
        initBuilder();
    }
}
