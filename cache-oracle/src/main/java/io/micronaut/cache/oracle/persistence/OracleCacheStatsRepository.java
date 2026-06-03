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

import io.micronaut.data.annotation.sql.Procedure;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
/**
 * Repository for lightweight cache statistics rows.
 *
 * @author Davide Cocco
 * @since 6.1.0
 */
@JdbcRepository(dialect = Dialect.ORACLE, dataSource = "${micronaut.oracle.cache.datasource}")
public interface OracleCacheStatsRepository extends CrudRepository<CacheStatsEntity, String> {

    /**
     * Applies cache statistics deltas.
     *
     * @param cacheName The cache name
     * @param hitDelta The cache hit count delta
     * @param missDelta The cache miss count delta
     * @param putDelta The cache put count delta
     * @param invalidateDelta The cache invalidation count delta
     * @param updatedAt The update timestamp
     */
    @Procedure("${micronaut.oracle.cache.prefix:MN}_CACHE_UPDATE_STATS")
    void updateStats(String cacheName,
                     long hitDelta,
                     long missDelta,
                     long putDelta,
                     long invalidateDelta,
                     Instant updatedAt);
}
