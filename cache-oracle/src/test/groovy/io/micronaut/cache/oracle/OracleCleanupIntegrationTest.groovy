/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.oracle

import io.micronaut.cache.oracle.persistence.CacheEntryEntity
import io.micronaut.cache.oracle.persistence.CacheEntryId
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers

import java.time.Instant

class OracleCleanupIntegrationTest extends OracleIntegrationSupport {

    void runCleanupUsesUtcComparisonIndependentlyFromSessionTimezone() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
        ])
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        when:
        try (def connection = openJdbcConnection();
             def insert = connection.prepareStatement('''
                 INSERT INTO MN_CACHE_ENTRY (
                     CACHE_NAME,
                     KEY_HASH,
                     KEY_PAYLOAD,
                     VALUE_PAYLOAD,
                     VALUE_WEIGHT,
                     CREATED_AT,
                     LAST_ACCESS_AT,
                     EXPIRES_AT
                 ) VALUES (
                     ?,
                     HEXTORAW('0102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F20'),
                     ?,
                     ?,
                     1,
                     SYSTIMESTAMP AT TIME ZONE 'UTC',
                     SYSTIMESTAMP AT TIME ZONE 'UTC',
                     (SYSTIMESTAMP AT TIME ZONE 'UTC') - INTERVAL '5' SECOND
                 )
             ''')) {
            insert.setString(1, 'orders')
            insert.setBytes(2, [1, 2, 3] as byte[])
            insert.setBytes(3, [4, 5, 6] as byte[])
            insert.executeUpdate()
        }

        try (def connection = openJdbcConnection();
             def statement = connection.createStatement()) {
            statement.execute("ALTER SESSION SET TIME_ZONE = 'Europe/Zurich'")
            statement.execute("BEGIN MN_CACHE_CLEANUP_CACHE('orders', 100); END;")
        }

        then:
        repository.countByIdCacheName('orders') == 0L

        cleanup:
        context.close()
    }

    void runCleanupRemovesExpiredEntries() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        // Build one expired row and one live row to ensure cleanup only removes the intended subset.
        repository.save(entry('orders', [1, 1, 1] as byte[], [3, 3, 3] as byte[], Instant.now().minusSeconds(30)))
        repository.save(entry('orders', [2, 2, 2] as byte[], [4, 4, 4] as byte[], Instant.now().plusSeconds(30)))

        when:
        cache.runCleanup()

        then:
        // Exactly one row should remain: the non-expired row.
        repository.countByIdCacheName('orders') == 1L

        cleanup:
        context.close()
    }

    void runCleanupKeepsRowsWhenNothingIsExpired() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        // Control case: all rows are valid, so cleanup should be a no-op.
        repository.save(entry('orders', [5, 5, 5] as byte[], [6, 6, 6] as byte[], Instant.now().plusSeconds(60)))

        when:
        cache.runCleanup()

        then:
        repository.countByIdCacheName('orders') == 1L

        cleanup:
        context.close()
    }

    void runCleanupWeightLimitRemovesOnlyRequiredRows() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
            'micronaut.caches.orders.cleanup-batch-size': 100,
            'micronaut.caches.orders.maximum-weight': 10,
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        repository.save(entry('orders', [1, 0, 0] as byte[], [1, 0, 0] as byte[], Instant.now().plusSeconds(60), 6L))
        repository.save(entry('orders', [2, 0, 0] as byte[], [2, 0, 0] as byte[], Instant.now().plusSeconds(60), 5L))
        repository.save(entry('orders', [3, 0, 0] as byte[], [3, 0, 0] as byte[], Instant.now().plusSeconds(60), 4L))

        expect:
        repository.totalWeight('orders') == 15L
        repository.countByIdCacheName('orders') == 3L

        when:
        cache.runCleanup()

        then:
        repository.totalWeight('orders') == 9L
        repository.countByIdCacheName('orders') == 2L

        cleanup:
        context.close()
    }

    void runCleanupTreatsNullWeightAsOne() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
            'micronaut.caches.orders.cleanup-batch-size': 100,
            'micronaut.caches.orders.maximum-weight': 1,
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        repository.save(entry('orders', [9, 0, 0] as byte[], [9, 0, 0] as byte[], Instant.now().plusSeconds(60), null))
        repository.save(entry('orders', [8, 0, 0] as byte[], [8, 0, 0] as byte[], Instant.now().plusSeconds(60), 1L))

        expect:
        repository.totalWeight('orders') == 2L

        when:
        cache.runCleanup()

        then:
        repository.totalWeight('orders') == 1L
        repository.countByIdCacheName('orders') == 1L

        cleanup:
        context.close()
    }

    private static CacheEntryEntity entry(String cacheName, byte[] keyHash, byte[] keyPayload, Instant expiresAt) {
        return entry(cacheName, keyHash, keyPayload, expiresAt, 1L)
    }

    private static CacheEntryEntity entry(String cacheName, byte[] keyHash, byte[] keyPayload, Instant expiresAt, Long valueWeight) {
        return new CacheEntryEntity(
            new CacheEntryId(cacheName, keyHash),
            keyPayload,
            'value'.bytes,
            valueWeight,
            Instant.now().minusSeconds(60),
            Instant.now().minusSeconds(60),
            expiresAt
        )
    }
}
