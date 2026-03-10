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
 */
@JdbcRepository(dialect = Dialect.ORACLE)
public interface OracleCacheEntryRepository extends CrudRepository<CacheEntryEntity, CacheEntryId> {

    Optional<CacheEntryEntity> findByIdCacheNameAndIdKeyHashAndIdKeyPayload(String cacheName, byte[] keyHash, byte[] keyPayload);

    @Procedure("MN_CACHE_PUT_BLOCKING")
    String blockingPut(String cacheName,
                       byte[] keyHash,
                       byte[] keyPayload,
                       byte[] valuePayload,
                       @Nullable Instant expiresAt,
                       long valueWeight);

    @Query(value = "UPDATE MN_CACHE_ENTRY SET LAST_ACCESS_AT = :lastAccessAt, EXPIRES_AT = :expiresAt WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash AND KEY_PAYLOAD = :keyPayload", nativeQuery = true)
    long updateLastAccess(String cacheName, byte[] keyHash, byte[] keyPayload, Instant lastAccessAt, @Nullable Instant expiresAt);

    @Query(value = "DELETE FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash AND KEY_PAYLOAD = :keyPayload", nativeQuery = true)
    long invalidateKey(String cacheName, byte[] keyHash, byte[] keyPayload);

    @Query(value = "DELETE FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long invalidateCache(String cacheName);

    @Query(value = "DELETE FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName AND EXPIRES_AT IS NOT NULL AND EXPIRES_AT < :now", nativeQuery = true)
    long deleteExpired(String cacheName, Instant now);

    @Query(value = "BEGIN MN_CACHE_CLEANUP_CACHE(:cacheName, :batchSize); END;", nativeQuery = true)
    void runCleanupProcedure(String cacheName, long batchSize);

    @Query(value = "BEGIN MN_CACHE_REGISTER_CLEANUP_JOB(:cacheName, :intervalSeconds); END;", nativeQuery = true)
    void registerCleanupJob(String cacheName, long intervalSeconds);

    @Query(value = """
        MERGE INTO MN_CACHE_CONFIG cfg
        USING (SELECT :cacheName AS CACHE_NAME FROM DUAL) incoming
        ON (cfg.CACHE_NAME = incoming.CACHE_NAME)
        WHEN MATCHED THEN UPDATE SET
            BLOCKING = :blocking,
            LOCK_WAIT_TIMEOUT_MS = :lockWaitTimeoutMs,
            CLEANUP_INTERVAL_SECONDS = :cleanupIntervalSeconds,
            MAXIMUM_SIZE = :maximumSize,
            MAXIMUM_WEIGHT = :maximumWeight,
            UPDATED_AT = :updatedAt
        WHEN NOT MATCHED THEN
            INSERT (CACHE_NAME, BLOCKING, LOCK_WAIT_TIMEOUT_MS, CLEANUP_INTERVAL_SECONDS, MAXIMUM_SIZE, MAXIMUM_WEIGHT, CREATED_AT, UPDATED_AT)
            VALUES (:cacheName, :blocking, :lockWaitTimeoutMs, :cleanupIntervalSeconds, :maximumSize, :maximumWeight, :createdAt, :updatedAt)
        """, nativeQuery = true)
    long upsertCacheConfig(String cacheName,
                           int blocking,
                           long lockWaitTimeoutMs,
                           long cleanupIntervalSeconds,
                           @Nullable Long maximumSize,
                           @Nullable Long maximumWeight,
                           Instant createdAt,
                           Instant updatedAt);

    long countByIdCacheName(String cacheName);

    @Query(value = "SELECT COALESCE(SUM(VALUE_WEIGHT), 0) FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long totalWeight(String cacheName);

}
