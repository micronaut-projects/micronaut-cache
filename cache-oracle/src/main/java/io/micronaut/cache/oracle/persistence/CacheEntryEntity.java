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

import io.micronaut.data.annotation.EmbeddedId;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

/**
 * Cache entry row persisted in Oracle.
 *
 * @param id The composite cache entry identifier
 * @param keyPayload The serialized cache key payload
 * @param valuePayload The serialized cache value payload
 * @param valueWeight The stored value weight
 * @param createdAt The row creation time
 * @param lastAccessAt The last access time
 * @param expiresAt The expiry time
 * @author Davide Cocco
 * @since 6.1.0
 */
@MappedEntity(value = "${micronaut.oracle.cache.prefix:MN}_CACHE_ENTRY")
public record CacheEntryEntity(
    @EmbeddedId CacheEntryId id,
    @MappedProperty("KEY_PAYLOAD") byte[] keyPayload,
    @MappedProperty("VALUE_PAYLOAD") byte[] valuePayload,
    @MappedProperty("VALUE_WEIGHT") Long valueWeight,
    @MappedProperty("CREATED_AT") Instant createdAt,
    @MappedProperty("LAST_ACCESS_AT") Instant lastAccessAt,
    @MappedProperty("EXPIRES_AT") Instant expiresAt
) {
}
