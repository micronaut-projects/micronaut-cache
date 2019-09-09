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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

import javax.annotation.Nonnull;

import static io.micronaut.cache.hazelcast.HazelcastConfiguration.PREFIX;

/**
 * Configuration class for an Hazelcast-based cache.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@EachProperty(PREFIX)
public class HazelcastConfiguration {

    public static final String PREFIX = "hazelcast.caches";
    public static final Integer DEFAULT_MAX_SIZE = 10;

    private final String name;

    private Integer maximumSize = DEFAULT_MAX_SIZE;
    private Integer backupCount;

    private Integer timeToLiveSeconds;

    /**
     * @param name the cache name
     */
    public HazelcastConfiguration(@Parameter String name) {
        this.name = name;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * @return The maximum number of entries
     */
    public Integer getMaximumSize() {
        return maximumSize;
    }

    /**
     * @param maximumSize The maximum number of entries
     */
    public void setMaximumSize(Integer maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Integer getBackupCount() {
        return backupCount;
    }

    public void setBackupCount(Integer backupCount) {
        this.backupCount = backupCount;
    }

    public Integer getTimeToLiveSeconds() { return timeToLiveSeconds; }

    public void setTimeToLiveSeconds(Integer timeToLiveSeconds) { this.timeToLiveSeconds = timeToLiveSeconds; }

}
