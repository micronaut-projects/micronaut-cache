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

import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration
import io.micronaut.cache.oracle.persistence.CacheEntryEntity
import io.micronaut.cache.oracle.persistence.CacheEntryId
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository
import io.micronaut.cache.oracle.serialization.OracleKeySerializer
import io.micronaut.context.ApplicationContext
import io.micronaut.core.convert.DefaultMutableConversionService
import io.micronaut.core.type.Argument
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class OracleSyncCacheTest extends Specification {

    void putPersistsEntryPayloadAndMetadata() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, serializer(), conversionService())

        when:
        cache.put('k1', 42)

        then:
        1 * repository.save({ CacheEntryEntity entity ->
            entity.id.cacheName == 'orders' &&
                    entity.valueWeight == 1L &&
                    entity.valuePayload != null &&
                    entity.createdAt != null &&
                    entity.lastAccessAt != null
        })
    }

    void getTreatsExpiredRowAsMiss() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, serializer(), conversionService())

        CacheEntryEntity expired = new CacheEntryEntity()
        expired.id = new CacheEntryId('orders', [1] as byte[], [2] as byte[])
        expired.valuePayload = '15'.bytes
        expired.createdAt = Instant.now().minusSeconds(120)
        expired.lastAccessAt = Instant.now().minusSeconds(120)
        expired.expiresAt = Instant.now().minusSeconds(1)

        and:
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.of(expired)

        when:
        Optional<Integer> result = cache.get('k2', Argument.of(Integer))

        then:
        result.empty
        1 * repository.invalidateKey('orders', _ as byte[], _ as byte[])
        0 * repository.updateLastAccess(_, _, _, _, _)
    }

    void nullPutInvalidatesKey() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, serializer(), conversionService())

        when:
        cache.put('k3', null)

        then:
        1 * repository.invalidateKey('orders', _ as byte[], _ as byte[])
        0 * repository.save(_)
    }

    void putIfAbsentReturnsExistingValue() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, serializer(), conversionService())

        CacheEntryEntity existing = new CacheEntryEntity()
        existing.id = new CacheEntryId('orders', [3] as byte[], [4] as byte[])
        existing.valuePayload = '99'.bytes
        existing.createdAt = Instant.now().minusSeconds(5)
        existing.lastAccessAt = Instant.now().minusSeconds(5)
        existing.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.of(existing)

        when:
        Optional<Integer> result = cache.putIfAbsent('k4', 10)

        then:
        result.present
        result.get() == 99
        1 * repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
    }

    void concurrentBlockingPutsCoordinateThroughDatabaseProcedure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, serializer(), conversionService())
        AtomicInteger supplierCalls = new AtomicInteger()
        AtomicInteger blockingCalls = new AtomicInteger()
        AtomicReference<CacheEntryEntity> stored = new AtomicReference<>()
        def supplier = {
            supplierCalls.incrementAndGet()
            Thread.sleep(100)
            return 5
        }

        and:
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> {
            return Optional.ofNullable(stored.get())
        }
        repository.blockingPut(_, _, _, _, _, _) >> {
            int call = blockingCalls.incrementAndGet()
            if (call == 1) {
                stored.set(storedEntity())
                return 'SUCCESS'
            }
            while (stored.get() == null) {
                Thread.sleep(5)
            }
            return 'ALREADY_INSERTED'
        }

        ExecutorService pool = Executors.newFixedThreadPool(2)
        CountDownLatch start = new CountDownLatch(1)

        when:
        def f1 = pool.submit {
            start.await(2, TimeUnit.SECONDS)
            return cache.get('blocking-key', Argument.of(Integer), supplier)
        }
        def f2 = pool.submit {
            start.await(2, TimeUnit.SECONDS)
            return cache.get('blocking-key', Argument.of(Integer), supplier)
        }
        start.countDown()

        Integer r1 = f1.get(3, TimeUnit.SECONDS)
        Integer r2 = f2.get(3, TimeUnit.SECONDS)

        then:
        r1 == 5
        r2 == 5
        supplierCalls.get() == 2
        blockingCalls.get() == 2

        cleanup:
        pool.shutdownNow()
    }

    void blockingPathReturnsPersistedValueWhenAlreadyInserted() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, serializer(), conversionService())

        CacheEntryEntity persisted = new CacheEntryEntity()
        persisted.id = new CacheEntryId('orders', [9] as byte[], [8] as byte[])
        persisted.valuePayload = '11'.bytes
        persisted.createdAt = Instant.now()
        persisted.lastAccessAt = Instant.now()
        persisted.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.blockingPut(_, _, _, _, _, _) >> 'ALREADY_INSERTED'
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.of(persisted)

        when:
        Integer value = cache.get('collision', Argument.of(Integer), { 11 })

        then:
        value == 11
    }

    void blockingPathRethrowsNonDuplicateFailure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, serializer(), conversionService())

        and:
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.empty()
        repository.blockingPut(_, _, _, _, _, _) >> { throw new IllegalStateException('connection lost') }

        when:
        cache.get('failure', Argument.of(Integer), { 15 })

        then:
        IllegalStateException ex = thrown()
        ex.message.contains('connection lost')
    }

    void blockingPutRollsbackOnFailure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, serializer(), conversionService())

        and:
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.empty()

        when:
        cache.get('rollback-key', Argument.of(Integer), { throw new IllegalStateException('write failed') })

        then:
        IllegalStateException ex = thrown()
        ex.message.contains('write failed')
        0 * repository.blockingPut(_, _, _, _, _, _)
    }

    void putIfAbsentReturnsExistingOnDuplicateInsert() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(false), repository, serializer(), conversionService())

        CacheEntryEntity existing = new CacheEntryEntity()
        existing.id = new CacheEntryId('orders', [1] as byte[], [2] as byte[])
        existing.valuePayload = '44'.bytes
        existing.createdAt = Instant.now().minusSeconds(5)
        existing.lastAccessAt = Instant.now().minusSeconds(5)
        existing.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        repository.findByIdCacheNameAndIdKeyHashAndIdKeyPayload(_, _, _) >> Optional.of(existing)

        when:
        Optional<Integer> result = cache.putIfAbsent('duplicate', 22)

        then:
        result.present
        result.get() == 44
    }

    void cleanupEnforcesSizeAndWeightBounds() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configurationWithLimits(), repository, serializer(), conversionService())

        when:
        cache.runCleanup()

        then:
        1 * repository.runCleanupProcedure('orders', 100L)
    }

    void exposesCacheInfoPayload() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, serializer(), conversionService())

        and:
        repository.countByIdCacheName('orders') >> 2L
        repository.totalWeight('orders') >> 9L

        when:
        def info = reactor.core.publisher.Flux.from(cache.cacheInfo).blockFirst()

        then:
        info.name == 'orders'
        info.get().implementationClass == 'io.micronaut.cache.oracle.OracleSyncCache'
        info.get().oracle.entryCount == 2L
        info.get().oracle.totalWeight == 9L
    }

    private static OracleCacheConfiguration configuration() {
        return configuration(false)
    }

    private static OracleCacheConfiguration configuration(boolean blocking) {
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.orders.expire-after-access': '30s',
                'micronaut.caches.orders.lock-wait-timeout' : '2s',
                'micronaut.caches.orders.blocking'          : blocking
        ])
        try {
            return context.getBean(OracleCacheConfiguration, Qualifiers.byName('orders'))
        } finally {
            context.close()
        }
    }

    private static OracleCacheConfiguration configurationWithLimits() {
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.orders.expire-after-access': '30s',
                'micronaut.caches.orders.lock-wait-timeout' : '2s',
                'micronaut.caches.orders.blocking'          : false,
                'micronaut.caches.orders.maximum-size'      : 1,
                'micronaut.caches.orders.maximum-weight'    : 10,
                'micronaut.caches.orders.cleanup-interval'  : '365d'
        ])
        try {
            return context.getBean(OracleCacheConfiguration, Qualifiers.byName('orders'))
        } finally {
            context.close()
        }
    }

    private static OracleKeySerializer serializer() {
        return new OracleKeySerializer(conversionService())
    }

    private static DefaultMutableConversionService conversionService() {
        return new DefaultMutableConversionService()
    }

    private static CacheEntryEntity storedEntity() {
        CacheEntryEntity entity = new CacheEntryEntity()
        entity.id = new CacheEntryId('orders', [3] as byte[], [4] as byte[])
        entity.valuePayload = '5'.bytes
        entity.createdAt = Instant.now()
        entity.lastAccessAt = Instant.now()
        entity.expiresAt = Instant.now().plusSeconds(20)
        return entity
    }
}
