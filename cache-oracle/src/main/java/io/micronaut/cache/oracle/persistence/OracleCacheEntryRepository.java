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
 * @since 6.0.0
 */
@JdbcRepository(dialect = Dialect.ORACLE, dataSource = "${micronaut.cache.oracle.datasource}")
public interface OracleCacheEntryRepository extends CrudRepository<CacheEntryEntity, CacheEntryId> {
    void update(@Id CacheEntryId id, Instant lastAccessAt, @Nullable Instant expiresAt);

    long delete(@Id CacheEntryId id);

    long countByCacheName(String cacheName);

    long deleteByCacheName(String cacheName);

    @Procedure("MN_CACHE_PUT_BLOCKING")
    String blockingPut(String cacheName,
                       byte[] keyHash,
                       byte[] keyPayload,
                       byte[] valuePayload,
                       @Nullable Instant expiresAt,
                       long valueWeight,
                       long insertOnly);

    @Procedure("MN_CACHE_CLEANUP_CACHE")
    void runCleanupProcedure(String cacheName, long batchSize);

    @Query(value = "SELECT COALESCE(SUM(COALESCE(VALUE_WEIGHT, 1)), 0) FROM MN_CACHE_ENTRY WHERE CACHE_NAME = :cacheName", nativeQuery = true)
    long totalWeight(String cacheName);

}
