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
 * @author Davide Cocco
 * @since 6.0.0
 */
@MappedEntity("MN_CACHE_ENTRY")
public final class CacheEntryEntity {

    @EmbeddedId
    private CacheEntryId id;

    @MappedProperty("KEY_PAYLOAD")
    private byte[] keyPayload;

    @MappedProperty("VALUE_PAYLOAD")
    private byte[] valuePayload;

    @MappedProperty("VALUE_WEIGHT")
    private Long valueWeight;

    @MappedProperty("CREATED_AT")
    private Instant createdAt;

    @MappedProperty("LAST_ACCESS_AT")
    private Instant lastAccessAt;

    @MappedProperty("EXPIRES_AT")
    private Instant expiresAt;

    public CacheEntryId getId() {
        return id;
    }

    public void setId(CacheEntryId id) {
        this.id = id;
    }

    public byte[] getValuePayload() {
        return valuePayload;
    }

    public byte[] getKeyPayload() {
        return keyPayload;
    }

    public void setKeyPayload(byte[] keyPayload) {
        this.keyPayload = keyPayload;
    }

    public void setValuePayload(byte[] valuePayload) {
        this.valuePayload = valuePayload;
    }

    public Long getValueWeight() {
        return valueWeight;
    }

    public void setValueWeight(Long valueWeight) {
        this.valueWeight = valueWeight;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastAccessAt() {
        return lastAccessAt;
    }

    public void setLastAccessAt(Instant lastAccessAt) {
        this.lastAccessAt = lastAccessAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
