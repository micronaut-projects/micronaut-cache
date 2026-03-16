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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.sql.Procedure;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for cache entry rows.
 *
 * @author Davide Cocco
 * @since 5.0.0
 */
@JdbcRepository(dialect = Dialect.ORACLE)
public interface OracleCacheEntryRepository extends CrudRepository<CacheEntryEntity, CacheEntryId> {

    @Query(value = "SELECT CACHE_NAME, KEY_HASH, KEY_PAYLOAD, VALUE_PAYLOAD, VALUE_WEIGHT, CREATED_AT, LAST_ACCESS_AT, EXPIRES_AT FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash", nativeQuery = true)
    Optional<CacheEntryEntity> findByIdCacheNameAndIdKeyHash(String cacheName, byte[] keyHash);

    @Procedure("MN_CACHE_PUT_BLOCKING")
    String blockingPut(String cacheName,
                       byte[] keyHash,
                       byte[] keyPayload,
                       byte[] valuePayload,
                       @Nullable Instant expiresAt,
                       long valueWeight);

    @Query(value = "UPDATE MN_CACHE_ENTRY SET LAST_ACCESS_AT = :lastAccessAt, EXPIRES_AT = :expiresAt WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash", nativeQuery = true)
    long updateLastAccess(String cacheName, byte[] keyHash, Instant lastAccessAt, @Nullable Instant expiresAt);

    @Query(value = "DELETE FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash", nativeQuery = true)
    long invalidateKey(String cacheName, byte[] keyHash);

    @Query(value = "DELETE FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long invalidateCache(String cacheName);

    @Procedure("MN_CACHE_CLEANUP_CACHE")
    void runCleanupProcedure(String cacheName, long batchSize);

    @Query(value = "SELECT COUNT(*) FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long countByIdCacheName(String cacheName);

    @Query(value = "SELECT COALESCE(SUM(COALESCE(VALUE_WEIGHT, 1)), 0) FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long totalWeight(String cacheName);

}
