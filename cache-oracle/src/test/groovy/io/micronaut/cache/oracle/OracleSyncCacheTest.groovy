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
import io.micronaut.cache.oracle.persistence.OracleCacheStatsRepository
import io.micronaut.cache.oracle.serialization.OracleKeySerializer
import io.micronaut.context.ApplicationContext
import io.micronaut.core.convert.DefaultMutableConversionService
import io.micronaut.core.type.Argument
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.json.JsonMapper
import io.micronaut.serde.annotation.Serdeable
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class OracleSyncCacheTest extends Specification {

    @Serdeable
    static class TestCar {
        String model
        int year
    }

    void getTreatsExpiredRowAsMiss() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), serializer(), jsonMapper())

        CacheEntryEntity expired = new CacheEntryEntity()
        expired.id = new CacheEntryId('orders', [1] as byte[])
        expired.keyPayload = [2] as byte[]
        expired.valuePayload = '15'.bytes
        expired.createdAt = Instant.now().minusSeconds(120)
        expired.lastAccessAt = Instant.now().minusSeconds(120)
        expired.expiresAt = Instant.now().minusSeconds(1)

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.of(expired)

        when:
        Optional<Integer> result = cache.get('k2', Argument.of(Integer))

        then:
        // Oracle cache should actively invalidate expired rows to avoid returning stale data forever.
        result.empty
        1 * repository.invalidateKey('orders', _ as byte[]) >> 1L
        // Once expired, access metadata should not be refreshed because entry is logically dead.
        0 * repository.updateLastAccess(_, _, _, _)
    }

    void concurrentBlockingPutsCoordinateThroughDatabaseProcedure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())
        AtomicInteger supplierCalls = new AtomicInteger()
        AtomicInteger blockingCalls = new AtomicInteger()
        AtomicReference<CacheEntryEntity> stored = new AtomicReference<>()
        def supplier = {
            supplierCalls.incrementAndGet()
            Thread.sleep(100)
            return 5
        }

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> {
            return Optional.ofNullable(stored.get())
        }
        repository.blockingPut(_, _, _, _, _, _) >> {
            int call = blockingCalls.incrementAndGet()
            // First caller wins and stores the row. Later callers observe ALREADY_INSERTED.
            if (call == 1) {
                stored.set(storedEntity())
                return 'SUCCESS'
            }
            // Simulate database-level coordination: second caller waits until first committed value exists.
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
        // Current implementation computes supplier in each caller before DB coordination.
        // This assertion captures current semantics and protects against accidental behavior changes.
        supplierCalls.get() == 2
        blockingCalls.get() == 2

        cleanup:
        pool.shutdownNow()
    }

    void blockingPathReturnsPersistedValueWhenAlreadyInserted() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())

        CacheEntryEntity persisted = new CacheEntryEntity()
        persisted.id = new CacheEntryId('orders', [9] as byte[])
        persisted.keyPayload = [8] as byte[]
        persisted.valuePayload = '11'.bytes
        persisted.createdAt = Instant.now()
        persisted.lastAccessAt = Instant.now()
        persisted.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.blockingPut(_, _, _, _, _, _) >> 'ALREADY_INSERTED'
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.of(persisted)

        when:
        Integer value = cache.get('collision', Argument.of(Integer), { 11 })

        then:
        // ALREADY_INSERTED means another contender has already committed the value.
        // Caller must read the persisted row and return it.
        value == 11
    }

    void blockingPathReplacesExistingValueWhenProcedureInsertCollides() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.empty()

        when:
        Integer value = cache.get('blocking-replace', Argument.of(Integer), { 12 })

        then:
        value == 12
        1 * repository.blockingPut(_, _, _, _, _, _) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        1 * repository.invalidateKey('orders', _ as byte[]) >> 1L
        1 * repository.blockingPut(_, _, _, _, _, _) >> 'SUCCESS'
    }

    void blockingPathRethrowsNonDuplicateFailure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.empty()
        repository.blockingPut(_, _, _, _, _, _) >> { throw new IllegalStateException('connection lost') }

        when:
        cache.get('failure', Argument.of(Integer), { 15 })

        then:
        // Only duplicate-key conflicts are recoverable; operational failures must bubble up.
        IllegalStateException ex = thrown()
        ex.message.contains('connection lost')
    }

    void blockingPutRollsbackOnFailure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.empty()

        when:
        cache.get('rollback-key', Argument.of(Integer), { throw new IllegalStateException('write failed') })

        then:
        // If supplier fails, cache must not attempt DB write coordination at all.
        IllegalStateException ex = thrown()
        ex.message.contains('write failed')
        0 * repository.blockingPut(_, _, _, _, _, _)
    }

    void putIfAbsentReturnsExistingOnDuplicateInsert() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(false), repository, statsRepository(), serializer(), jsonMapper())

        CacheEntryEntity existing = new CacheEntryEntity()
        existing.id = new CacheEntryId('orders', [1] as byte[])
        existing.keyPayload = [2] as byte[]
        existing.valuePayload = '44'.bytes
        existing.createdAt = Instant.now().minusSeconds(5)
        existing.lastAccessAt = Instant.now().minusSeconds(5)
        existing.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.of(existing)

        when:
        Optional<Integer> result = cache.putIfAbsent('duplicate', 22)

        then:
        // Duplicate insert path should degrade to "read existing" instead of surfacing ORA-00001.
        result.present
        result.get() == 44
    }

    void putSerializesObjectValueAsJsonBytes() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), serializer(), jsonMapper())

        when:
        cache.put('json-key', new TestCar(model: 'Hello', year: 2026))

        then:
        // We store JSON bytes (not toString text) so object payloads can be deserialized later.
        1 * repository.save({ CacheEntryEntity entity ->
            String payload = new String(entity.valuePayload, StandardCharsets.UTF_8)
            payload.contains('"model":"Hello"') && payload.contains('"year":2026')
        })
    }

    void putReplacesExistingValueWhenInsertCollides() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), serializer(), jsonMapper())

        when:
        cache.put('replace-key', 4)

        then:
        1 * repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        1 * repository.invalidateKey('orders', _ as byte[]) >> 1L
        1 * repository.save({ CacheEntryEntity entity ->
            new String(entity.valuePayload, StandardCharsets.UTF_8) == '4'
        })
    }

    void getFailsFastWhenStoredPayloadIsNotValidJsonForRequestedType() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), serializer(), jsonMapper())

        CacheEntryEntity existing = new CacheEntryEntity()
        existing.id = new CacheEntryId('orders', [7] as byte[])
        existing.keyPayload = [8] as byte[]
        // Simulates legacy/non-JSON text payload previously observed in integration debugging.
        existing.valuePayload = 'Car[model=Hello,year=2026]'.bytes
        existing.createdAt = Instant.now().minusSeconds(5)
        existing.lastAccessAt = Instant.now().minusSeconds(5)
        existing.expiresAt = Instant.now().plusSeconds(30)

        and:
        repository.findByIdCacheNameAndIdKeyHash(_, _) >> Optional.of(existing)

        when:
        cache.get('json-key', Argument.of(TestCar))

        then:
        IllegalStateException ex = thrown()
        ex.message.contains('Failed to decode cache value as JSON')
    }

    void cleanupEnforcesSizeAndWeightBounds() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configurationWithLimits(), repository, statsRepository(), serializer(), jsonMapper())

        when:
        cache.runCleanup()

        then:
        // Cache delegates retention policy enforcement to Oracle cleanup procedure.
        1 * repository.runCleanupProcedure('orders', 37L)
    }

    void exposesCacheInfoPayload() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), serializer(), jsonMapper())

        and:
        repository.countByIdCacheName('orders') >> 2L
        repository.totalWeight('orders') >> 9L

        when:
        def info = reactor.core.publisher.Flux.from(cache.cacheInfo).blockFirst()

        then:
        // CacheInfo bridges runtime cache metadata with Oracle-specific metrics from repository queries.
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
                'micronaut.caches.orders.cleanup-interval'  : '365d',
                'micronaut.caches.orders.cleanup-batch-size': 37
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

    private static JsonMapper jsonMapper() {
        return JsonMapper.createDefault()
    }

    private OracleCacheStatsRepository statsRepository() {
        return Mock(OracleCacheStatsRepository) {
            _ * updateStats(_, _, _, _, _, _)
        }
    }

    private static CacheEntryEntity storedEntity() {
        CacheEntryEntity entity = new CacheEntryEntity()
        entity.id = new CacheEntryId('orders', [3] as byte[])
        entity.keyPayload = [4] as byte[]
        entity.valuePayload = '5'.bytes
        entity.createdAt = Instant.now()
        entity.lastAccessAt = Instant.now()
        entity.expiresAt = Instant.now().plusSeconds(20)
        return entity
    }
}
