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
 * @since 6.0.0
 */
@MappedEntity("MN_CACHE_CONFIG")
public record CacheConfigEntity(
    @Id @MappedProperty("CACHE_NAME") String cacheName,
    @MappedProperty("BLOCKING") boolean blocking,
    @MappedProperty("LOCK_WAIT_TIMEOUT_MS") long lockWaitTimeoutMs,
    @MappedProperty("CLEANUP_INTERVAL_SECONDS") long cleanupIntervalSeconds,
    @MappedProperty("CLEANUP_BATCH_SIZE") long cleanupBatchSize,
    @MappedProperty("MAXIMUM_SIZE") Long maximumSize,
    @MappedProperty("MAXIMUM_WEIGHT") Long maximumWeight,
    @MappedProperty("CREATED_AT") Instant createdAt,
    @MappedProperty("UPDATED_AT") Instant updatedAt
) {
}
