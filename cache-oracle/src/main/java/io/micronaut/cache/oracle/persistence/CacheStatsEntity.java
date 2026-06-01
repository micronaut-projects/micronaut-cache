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
package io.micronaut.cache.oracle.persistence;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

/**
 * Persisted cache statistics row.
 *
 * @param cacheName The cache name
 * @param hitCount The cache hit count
 * @param missCount The cache miss count
 * @param putCount The cache put count
 * @param invalidateCount The cache invalidate count
 * @param updatedAt The last update time
 * @author Davide Cocco
 * @since 6.0.0
 */
@MappedEntity(value = "${micronaut.oracle.cache.prefix:MN}_CACHE_STATS")
public record CacheStatsEntity(
    @Id @MappedProperty("CACHE_NAME") String cacheName,
    @MappedProperty("HIT_COUNT") long hitCount,
    @MappedProperty("MISS_COUNT") long missCount,
    @MappedProperty("PUT_COUNT") long putCount,
    @MappedProperty("INVALIDATE_COUNT") long invalidateCount,
    @MappedProperty("UPDATED_AT") Instant updatedAt
) {
}
