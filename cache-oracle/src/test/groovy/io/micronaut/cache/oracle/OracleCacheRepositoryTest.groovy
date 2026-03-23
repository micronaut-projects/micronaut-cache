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

        CacheEntryEntity entity = new CacheEntryEntity(
            new CacheEntryId('orders', hash),
            payload,
            'value-1'.bytes,
            10L,
            Instant.now(),
            Instant.now(),
            Instant.now().plusSeconds(60)
        )

        repository.save(entity)

        when:
        // Critical repository behavior: lookups must be hash-based to avoid LOB payload comparisons.
        CacheEntryEntity fetched = repository.findById(new CacheEntryId('orders', hash)).orElse(null)

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

        CacheEntryEntity first = new CacheEntryEntity(
            new CacheEntryId('orders', sameHash),
            payloadA,
            'a'.bytes,
            1L,
            Instant.now(),
            Instant.now(),
            Instant.now().plusSeconds(120)
        )

        CacheEntryEntity second = new CacheEntryEntity(
            new CacheEntryId('orders', sameHash),
            payloadB,
            'b'.bytes,
            2L,
            Instant.now(),
            Instant.now(),
            Instant.now().plusSeconds(120)
        )

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

    void invalidationQueriesWork() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        byte[] hash = [7, 7, 7] as byte[]
        byte[] payload = [8, 8, 8] as byte[]

        CacheEntryEntity expiring = new CacheEntryEntity(
            new CacheEntryId('orders', hash),
            payload,
            'expiring'.bytes,
            1L,
            Instant.now().minusSeconds(30),
            Instant.now().minusSeconds(30),
            Instant.now().minusSeconds(5)
        )
        repository.save(expiring)

        when:
        repository.delete(new CacheEntryId('orders', hash))
        CacheEntryEntity deletedMissing = repository.findById(new CacheEntryId('orders', hash)).orElse(null)

        then:
        repository.findById(new CacheEntryId('orders', hash)).empty
        deletedMissing == null

        cleanup:
        context.close()
    }
}
