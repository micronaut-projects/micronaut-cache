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

    void runCleanupRemovesExpiredEntries() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '5s',
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')

        repository.save(entry('orders', [1, 1, 1] as byte[], [3, 3, 3] as byte[], Instant.now().minusSeconds(30)))
        repository.save(entry('orders', [2, 2, 2] as byte[], [4, 4, 4] as byte[], Instant.now().plusSeconds(30)))

        when:
        cache.runCleanup()

        then:
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

        repository.save(entry('orders', [5, 5, 5] as byte[], [6, 6, 6] as byte[], Instant.now().plusSeconds(60)))

        when:
        cache.runCleanup()

        then:
        repository.countByIdCacheName('orders') == 1L

        cleanup:
        context.close()
    }

    private static CacheEntryEntity entry(String cacheName, byte[] keyHash, byte[] keyPayload, Instant expiresAt) {
        CacheEntryEntity entity = new CacheEntryEntity()
        entity.id = new CacheEntryId(cacheName, keyHash)
        entity.keyPayload = keyPayload
        entity.valuePayload = 'value'.bytes
        entity.valueWeight = 1L
        entity.createdAt = Instant.now().minusSeconds(60)
        entity.lastAccessAt = Instant.now().minusSeconds(60)
        entity.expiresAt = expiresAt
        return entity
    }
}
