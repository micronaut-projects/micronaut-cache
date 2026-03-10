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
import io.micronaut.cache.oracle.schema.OracleCacheSchemaInitializer
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.OracleContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Instant
import java.util.Arrays

@Testcontainers
@Stepwise
@IgnoreIf({ !new File('/var/run/docker.sock').exists() && !System.getenv('DOCKER_HOST') })
class OracleCacheRepositoryTest extends Specification {

    @Shared
    OracleContainer oracle = new OracleContainer('gvenzl/oracle-xe:21-slim-faststart')

    void putAndFetchByHashAndPayload() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)

        byte[] hash = [1, 2, 3] as byte[]
        byte[] payload = [4, 5, 6] as byte[]

        CacheEntryEntity entity = new CacheEntryEntity()
        entity.id = new CacheEntryId('orders', hash, payload)
        entity.valuePayload = 'value-1'.bytes
        entity.valueWeight = 10L
        entity.createdAt = Instant.now()
        entity.lastAccessAt = Instant.now()
        entity.expiresAt = Instant.now().plusSeconds(60)

        repository.save(entity)

        when:
        CacheEntryEntity fetched = repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload('orders', hash, payload).orElse(null)

        then:
        fetched != null
        fetched.id == entity.id
        Arrays.equals(fetched.valuePayload, entity.valuePayload)
        fetched.valueWeight == 10L

        cleanup:
        context.close()
    }

    void hashCollisionDoesNotReturnWrongPayload() {
        given:
        ApplicationContext context = newContext()
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)

        byte[] sameHash = [9, 9, 9] as byte[]
        byte[] payloadA = [1, 1, 1] as byte[]
        byte[] payloadB = [2, 2, 2] as byte[]

        CacheEntryEntity first = new CacheEntryEntity()
        first.id = new CacheEntryId('orders', sameHash, payloadA)
        first.valuePayload = 'a'.bytes
        first.valueWeight = 1L
        first.createdAt = Instant.now()
        first.lastAccessAt = Instant.now()
        first.expiresAt = Instant.now().plusSeconds(120)

        CacheEntryEntity second = new CacheEntryEntity()
        second.id = new CacheEntryId('orders', sameHash, payloadB)
        second.valuePayload = 'b'.bytes
        second.valueWeight = 2L
        second.createdAt = Instant.now()
        second.lastAccessAt = Instant.now()
        second.expiresAt = Instant.now().plusSeconds(120)

        repository.save(first)
        repository.save(second)

        when:
        CacheEntryEntity fetched = repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload('orders', sameHash, payloadB).orElse(null)
        CacheEntryEntity wrong = repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload('orders', sameHash, payloadA).orElse(null)

        then:
        fetched != null
        wrong != null
        Arrays.equals(fetched.id.keyPayload, payloadB)
        Arrays.equals(wrong.id.keyPayload, payloadA)

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
        expiring.id = new CacheEntryId('orders', hash, payload)
        expiring.valuePayload = 'expiring'.bytes
        expiring.valueWeight = 1L
        expiring.createdAt = Instant.now().minusSeconds(30)
        expiring.lastAccessAt = Instant.now().minusSeconds(30)
        expiring.expiresAt = Instant.now().minusSeconds(5)
        repository.save(expiring)

        when:
        long deletedExpired = repository.deleteExpired('orders', Instant.now())
        long deletedMissing = repository.invalidateKey('orders', hash, payload)

        then:
        deletedExpired == 1
        deletedMissing == 0

        cleanup:
        context.close()
    }

    private ApplicationContext newContext() {
        ApplicationContext context = ApplicationContext.run([
                'datasources.default.url'                        : oracle.jdbcUrl,
                'datasources.default.username'                   : oracle.username,
                'datasources.default.password'                   : oracle.password,
                'datasources.default.driver-class-name'          : 'oracle.jdbc.OracleDriver',
                'micronaut.caches.orders.expire-after-access'   : '30s',
                'micronaut.caches.orders.expire-after-write'    : '2m',
                'micronaut.caches.orders.record-stats'          : true,
                'micronaut.caches.orders.test-mode'             : true
        ])
        context.getBean(OracleCacheSchemaInitializer).initializeSchema()
        return context
    }
}
