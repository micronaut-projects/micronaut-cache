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

import io.micronaut.core.convert.format.ReadableBytes;

/**
 * Common resource pools configuration properties.
 */
public abstract class AbstractResourcePoolConfiguration {
    private Long maxSize;

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
}
