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
package io.micronaut.cache.ehcache.configuration;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;

import javax.annotation.Nonnull;

/**
 * Resource pool configurations.
 */
@EachProperty(EhcacheClusterResourcePoolConfiguration.PREFIX)
public class EhcacheClusterResourcePoolConfiguration extends AbstractResourcePoolConfiguration implements Named {
    public static final String PREFIX = EhcacheCacheManagerConfiguration.PREFIX + "." + EhcacheCacheManagerConfiguration.EhcacheClusterConfiguration.PREFIX +  ".resource-pools";

    private final String name;
    private String serverResource;

    /**
     * @param name the resource pool name
     */
    public EhcacheClusterResourcePoolConfiguration(@Parameter String name) {
        this.name = name;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

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
