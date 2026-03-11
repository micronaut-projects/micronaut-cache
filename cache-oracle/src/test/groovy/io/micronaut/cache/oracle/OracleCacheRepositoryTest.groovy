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
import spock.lang.Stepwise

import java.time.Instant
import java.util.Arrays

@Stepwise
class OracleCacheRepositoryTest extends OracleIntegrationSupport {

    void putAndFetchByHashAndPayload() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)

        byte[] hash = [1, 2, 3] as byte[]
        byte[] payload = [4, 5, 6] as byte[]

        CacheEntryEntity entity = new CacheEntryEntity()
        entity.id = new CacheEntryId('orders', hash)
        entity.keyPayload = payload
        entity.valuePayload = 'value-1'.bytes
        entity.valueWeight = 10L
        entity.createdAt = Instant.now()
        entity.lastAccessAt = Instant.now()
        entity.expiresAt = Instant.now().plusSeconds(60)

        repository.save(entity)

        when:
        // Critical repository behavior: lookups must be hash-based to avoid LOB payload comparisons.
        CacheEntryEntity fetched = repository.findByIdCacheNameAndIdKeyHash('orders', hash).orElse(null)

        then:
        fetched != null
        fetched.id == entity.id
        Arrays.equals(fetched.keyPayload, payload)
        Arrays.equals(fetched.valuePayload, entity.valuePayload)
        fetched.valueWeight == 10L

        cleanup:
        context.close()
    }

    void duplicateHashInsertIsRejected() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)

        byte[] sameHash = [9, 9, 9] as byte[]
        byte[] payloadA = [1, 1, 1] as byte[]
        byte[] payloadB = [2, 2, 2] as byte[]

        CacheEntryEntity first = new CacheEntryEntity()
        first.id = new CacheEntryId('orders', sameHash)
        first.keyPayload = payloadA
        first.valuePayload = 'a'.bytes
        first.valueWeight = 1L
        first.createdAt = Instant.now()
        first.lastAccessAt = Instant.now()
        first.expiresAt = Instant.now().plusSeconds(120)

        CacheEntryEntity second = new CacheEntryEntity()
        second.id = new CacheEntryId('orders', sameHash)
        second.keyPayload = payloadB
        second.valuePayload = 'b'.bytes
        second.valueWeight = 2L
        second.createdAt = Instant.now()
        second.lastAccessAt = Instant.now()
        second.expiresAt = Instant.now().plusSeconds(120)

        repository.save(first)

        when:
        // Oracle PK is (cache_name, key_hash), so second insert with same hash must fail.
        repository.save(second)

        then:
        RuntimeException ex = thrown()
        ex.message.contains('ORA-00001')

        cleanup:
        context.close()
    }

    void invalidationAndExpirationQueriesWork() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        byte[] hash = [7, 7, 7] as byte[]
        byte[] payload = [8, 8, 8] as byte[]

        CacheEntryEntity expiring = new CacheEntryEntity()
        expiring.id = new CacheEntryId('orders', hash)
        expiring.keyPayload = payload
        expiring.valuePayload = 'expiring'.bytes
        expiring.valueWeight = 1L
        expiring.createdAt = Instant.now().minusSeconds(30)
        expiring.lastAccessAt = Instant.now().minusSeconds(30)
        expiring.expiresAt = Instant.now().minusSeconds(5)
        repository.save(expiring)

        when:
        // deleteExpired should remove stale rows, then invalidateKey should report no-op for missing row.
        long deletedExpired = repository.deleteExpired('orders', Instant.now())
        long deletedMissing = repository.invalidateKey('orders', hash)

        then:
        deletedExpired == 1
        deletedMissing == 0

        cleanup:
        context.close()
    }
}
