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
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.sql.Procedure;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
/**
 * Repository for cache entry rows.
 *
 * @author Davide Cocco
 * @since 6.1.0
 */
@JdbcRepository(dialect = Dialect.ORACLE, dataSource = "${micronaut.oracle.cache.datasource}")
public interface OracleCacheEntryRepository extends CrudRepository<CacheEntryEntity, CacheEntryId> {
    /**
     * Updates access metadata for a cache entry.
     *
     * @param id The cache entry id
     * @param lastAccessAt The last access timestamp
     * @param expiresAt The next expiration timestamp
     */
    void update(@Id CacheEntryId id, Instant lastAccessAt, @Nullable Instant expiresAt);

    /**
     * Deletes a cache entry by id.
     *
     * @param id The cache entry id
     * @return The number of deleted rows
     */
    long delete(@Id CacheEntryId id);

    /**
     * Counts entries for a cache.
     *
     * @param cacheName The cache name
     * @return The number of entries
     */
    long countByCacheName(String cacheName);

    /**
     * Deletes all entries for a cache.
     *
     * @param cacheName The cache name
     * @return The number of deleted rows
     */
    long deleteByCacheName(String cacheName);

    /**
     * Writes a value using the database-coordinated blocking put procedure.
     *
     * @param cacheName The cache name
     * @param keyHash The cache key hash
     * @param keyPayload The serialized cache key payload
     * @param valuePayload The serialized cache value payload
     * @param expiresAt The expiration timestamp
     * @param valueWeight The value weight
     * @param insertOnly Whether to only insert when absent
     * @return The procedure status
     */
    @Procedure("${micronaut.oracle.cache.prefix:MN}_CACHE_PUT_BLOCKING")
    String blockingPut(String cacheName,
                       byte[] keyHash,
                       byte[] keyPayload,
                       byte[] valuePayload,
                       @Nullable Instant expiresAt,
                       long valueWeight,
                       long insertOnly);

    /**
     * Runs the database cleanup procedure for a cache.
     *
     * @param cacheName The cache name
     * @param batchSize The cleanup batch size
     */
    @Procedure("${micronaut.oracle.cache.prefix:MN}_CACHE_CLEANUP_CACHE")
    void runCleanupProcedure(String cacheName, long batchSize);

    /**
     * Calculates the total weight for entries in a cache.
     *
     * @param cacheName The cache name
     * @return The total entry weight
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(VALUE_WEIGHT, 1)), 0) FROM ${micronaut.oracle.cache.prefix:`MN`}_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long totalWeight(String cacheName);

}
