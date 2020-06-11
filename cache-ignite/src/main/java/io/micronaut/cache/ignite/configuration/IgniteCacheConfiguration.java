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
package io.micronaut.cache.ignite.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Ignite cache configuration.
 */
@EachProperty(value = "ignite.caches", primary = "default")
public class IgniteCacheConfiguration implements Named {
    private final String name;
    private String client = "default";

    @ConfigurationBuilder(excludes = {"Name"})
    private final CacheConfiguration configuration = new CacheConfiguration();

    /**
     * @param name Name or key for client.
     */
    public IgniteCacheConfiguration(@Parameter String name) {
        this.name = name;
    }

    /**
     * @param client name of client to reference when building cache.
     */
    public void setClient(String client) {
        this.client = client;
    }

    /**
     * @return  name of client to reference when building cache.
     */
    public String getClient() {
        return client;
    }

    /**
     * @return ignite cache configuration.
     */
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return build cache configuration from base and properties.
     */
    public CacheConfiguration build() {
        return new CacheConfiguration(configuration).setName(this.name);
    }

    /**
     * @return name or key for client.
     */
    @NonNull
    public String getName() {
        return this.name;
    }
}
