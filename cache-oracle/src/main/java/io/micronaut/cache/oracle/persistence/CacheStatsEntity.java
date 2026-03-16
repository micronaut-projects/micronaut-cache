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
 */
@MappedEntity("MN_CACHE_STATS")
public final class CacheStatsEntity {

    @Id
    @MappedProperty("CACHE_NAME")
    private String cacheName;

    @MappedProperty("HIT_COUNT")
    private long hitCount;

    @MappedProperty("MISS_COUNT")
    private long missCount;

    @MappedProperty("PUT_COUNT")
    private long putCount;

    @MappedProperty("INVALIDATE_COUNT")
    private long invalidateCount;

    @MappedProperty("UPDATED_AT")
    private Instant updatedAt;

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public void setMissCount(long missCount) {
        this.missCount = missCount;
    }

    public long getPutCount() {
        return putCount;
    }

    public void setPutCount(long putCount) {
        this.putCount = putCount;
    }

    public long getInvalidateCount() {
        return invalidateCount;
    }

    public void setInvalidateCount(long invalidateCount) {
        this.invalidateCount = invalidateCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
