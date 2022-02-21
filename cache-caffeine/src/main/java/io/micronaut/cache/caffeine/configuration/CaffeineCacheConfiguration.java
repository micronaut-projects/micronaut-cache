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
package io.micronaut.cache.caffeine.configuration;

import io.micronaut.cache.CacheConfiguration;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.runtime.ApplicationConfiguration;

/**
 * Default cache configuration implementation used to configure instances of {@link DefaultSyncCache}.
 *
 * @author graemerocher
 * @since 1.0.2
 */
public class CaffeineCacheConfiguration extends CacheConfiguration {

    private boolean listenToRemovals;
    private boolean listenToEvictions;

    /**
     * Creates a new cache with the given name.
     *
     * @param cacheName                Name or key of the cache
     * @param applicationConfiguration The common application configuration
     */
    public CaffeineCacheConfiguration(@Parameter String cacheName, ApplicationConfiguration applicationConfiguration) {
        super(cacheName, applicationConfiguration);
    }

    /**
     * If a removal listener is defined and this property is true then caffeine will send removal events to that listener.
     *
     * @return True if listen to removals is enabled
     */
    public boolean isListenToRemovals() {
        return this.listenToRemovals;
    }

    /**
     *
     * @param listenToRemovals The listen to removals flag.
     */
    public void setListenToRemovals(boolean listenToRemovals) {
        this.listenToRemovals = listenToRemovals;
    }

    /**
     * If a removal listener is defined and this property is true then caffeine will send eviction events to that listener.
     *
     * @return True if listen to evictions is enabled
     */
    public boolean isListenToEvictions() {
        return this.listenToEvictions;
    }

    /**
     *
     * @param listenToEvictions The listen to evictions flag.
     */
    public void setListenToEvictions(boolean listenToEvictions) {
        this.listenToEvictions = listenToEvictions;
    }
}
