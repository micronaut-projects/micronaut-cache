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
 * Persisted per-cache Oracle configuration row.
 * @author Davide Cocco
 * @since 5.0.0
 */
@MappedEntity("MN_CACHE_CONFIG")
public final class CacheConfigEntity {

    @Id
    @MappedProperty("CACHE_NAME")
    private String cacheName;

    @MappedProperty("BLOCKING")
    private boolean blocking;

    @MappedProperty("LOCK_WAIT_TIMEOUT_MS")
    private long lockWaitTimeoutMs;

    @MappedProperty("CLEANUP_INTERVAL_SECONDS")
    private long cleanupIntervalSeconds;

    @MappedProperty("CLEANUP_BATCH_SIZE")
    private long cleanupBatchSize;

    @MappedProperty("MAXIMUM_SIZE")
    private Long maximumSize;

    @MappedProperty("MAXIMUM_WEIGHT")
    private Long maximumWeight;

    @MappedProperty("CREATED_AT")
    private Instant createdAt;

    @MappedProperty("UPDATED_AT")
    private Instant updatedAt;

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public long getLockWaitTimeoutMs() {
        return lockWaitTimeoutMs;
    }

    public void setLockWaitTimeoutMs(long lockWaitTimeoutMs) {
        this.lockWaitTimeoutMs = lockWaitTimeoutMs;
    }

    public long getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public void setCleanupIntervalSeconds(long cleanupIntervalSeconds) {
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }

    public long getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(long cleanupBatchSize) {
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public Long getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(Long maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Long getMaximumWeight() {
        return maximumWeight;
    }

    public void setMaximumWeight(Long maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
