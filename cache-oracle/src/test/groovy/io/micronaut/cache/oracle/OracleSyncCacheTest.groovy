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
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.serde.oracle.jdbc.json.OracleJdbcJsonBinaryObjectMapper
import spock.lang.Specification

import java.io.IOException
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
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        CacheEntryEntity expired = new CacheEntryEntity(
            new CacheEntryId('orders', [1] as byte[]),
            [2] as byte[],
            '15'.bytes,
            null,
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(1)
        )

        and:
        repository.findById(_ as CacheEntryId) >> Optional.of(expired)

        when:
        Optional<Integer> result = cache.get('k2', Argument.of(Integer))

        then:
        // Oracle cache should actively invalidate expired rows to avoid returning stale data forever.
        result.empty
        1 * repository.delete(_ as CacheEntryId)
        // Once expired, access metadata should not be refreshed because entry is logically dead.
        0 * repository.updateLastAccess(_, _, _, _)
    }

    void concurrentBlockingPutsCoordinateThroughDatabaseProcedure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())
        AtomicInteger supplierCalls = new AtomicInteger()
        AtomicInteger blockingCalls = new AtomicInteger()
        AtomicReference<CacheEntryEntity> stored = new AtomicReference<>()
        def supplier = {
            supplierCalls.incrementAndGet()
            Thread.sleep(100)
            return 5
        }

        and:
        repository.findById(_ as CacheEntryId) >> {
            return Optional.ofNullable(stored.get())
        }
        repository.blockingPut(_, _, _, _, _, _, 0L) >> {
            int call = blockingCalls.incrementAndGet()
            // First caller inserts, later callers update after Oracle serialization.
            if (call == 1) {
                stored.set(storedEntity())
                return 'INSERTED'
            }
            // Simulate database-level coordination: second caller waits until first committed value exists,
            // then overwrites it.
            while (stored.get() == null) {
                Thread.sleep(5)
            }
            return 'UPDATED_PAYLOAD'
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

    void blockingPutAcceptsUpdatedStatusAsSuccessfulOverwrite() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        and:
        repository.blockingPut(_, _, _, _, _, _, 0L) >> 'UPDATED'

        when:
        cache.put('collision', 12)

        then:
        noExceptionThrown()
    }

    void blockingPutTreatsInsertedStatusAsSuccessfulWrite() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        and:
        repository.blockingPut(_, _, _, _, _, _, 0L) >> 'INSERTED'

        when:
        cache.put('blocking-replace', 12)

        then:
        noExceptionThrown()
    }

    void blockingPathRethrowsNonDuplicateFailure() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        and:
        repository.findById(_ as CacheEntryId) >> Optional.empty()
        repository.blockingPut(_, _, _, _, _, _, 0L) >> { throw new IllegalStateException('connection lost') }

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
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        and:
        repository.findById(_ as CacheEntryId) >> Optional.empty()

        when:
        cache.get('rollback-key', Argument.of(Integer), { throw new IllegalStateException('write failed') })

        then:
        // If supplier fails, cache must not attempt DB write coordination at all.
        IllegalStateException ex = thrown()
        ex.message.contains('write failed')
        0 * repository.blockingPut(_, _, _, _, _, _, _)
    }

    void putIfAbsentReturnsExistingOnDuplicateInsert() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(false), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        CacheEntryEntity existing = new CacheEntryEntity(
            new CacheEntryId('orders', [1] as byte[]),
            [2] as byte[],
            jsonBytes(44),
            null,
            Instant.now().minusSeconds(5),
            Instant.now().minusSeconds(5),
            Instant.now().plusSeconds(30)
        )

        and:
        repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        repository.findById(_ as CacheEntryId) >> Optional.of(existing)

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
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        when:
        cache.put('json-key', new TestCar(model: 'Hello', year: 2026))

        then:
        // We store Oracle binary JSON bytes so object payloads can be deserialized later.
        1 * repository.save({ CacheEntryEntity entity ->
            TestCar car = jsonMapper().readValue(entity.valuePayload, TestCar)
            car.model == 'Hello' && car.year == 2026
        })
    }

    void putReplacesExistingValueWhenInsertCollides() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        when:
        cache.put('replace-key', 4)

        then:
        1 * repository.save(_ as CacheEntryEntity) >> { throw new IllegalStateException('ORA-00001: unique constraint') }
        1 * repository.delete(_ as CacheEntryId)
        1 * repository.save({ CacheEntryEntity entity ->
            jsonMapper().readValue(entity.valuePayload, Integer) == 4
        })
    }

    void getFailsFastWhenStoredPayloadIsNotValidJsonForRequestedType() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        CacheEntryEntity existing = new CacheEntryEntity(
            new CacheEntryId('orders', [7] as byte[]),
            [8] as byte[],
            [1, 2] as byte[],
            null,
            Instant.now().minusSeconds(5),
            Instant.now().minusSeconds(5),
            Instant.now().plusSeconds(30)
        )

        and:
        repository.findById(_ as CacheEntryId) >> Optional.of(existing)

        when:
        cache.get('json-key', Argument.of(TestCar))

        then:
        IllegalStateException ex = thrown()
        ex.message.contains('Failed to decode cache value as JSON')
    }

    void cleanupEnforcesSizeAndWeightBounds() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configurationWithLimits(), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        when:
        cache.runCleanup()

        then:
        // Cache delegates retention policy enforcement to Oracle cleanup procedure.
        1 * repository.runCleanupProcedure('orders', 37L)
    }

    void exposesCacheInfoPayload() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configuration(true), repository, statsRepository(), executorService(), serializer(), jsonMapper())

        and:
        repository.countByCacheName('orders') >> 2L
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
                'micronaut.oracle.cache.datasource'      : 'default',
                'micronaut.oracle.cache.prefix'          : 'MN',
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
                'micronaut.oracle.cache.datasource'      : 'default',
                'micronaut.oracle.cache.prefix'          : 'MN',
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
        return new OracleKeySerializer(jsonMapper(), conversionService())
    }

    private static DefaultMutableConversionService conversionService() {
        return new DefaultMutableConversionService()
    }

    private static OracleJdbcJsonBinaryObjectMapper jsonMapper() {
        ApplicationContext context = ApplicationContext.run()
        try {
            return context.getBean(OracleJdbcJsonBinaryObjectMapper)
        } finally {
            context.close()
        }
    }

    private static byte[] jsonBytes(Object value) {
        try {
            return jsonMapper().writeValueAsBytes(value)
        } catch (IOException e) {
            throw new IllegalStateException(e)
        }
    }

    private OracleCacheStatsRepository statsRepository() {
        return Mock(OracleCacheStatsRepository) {
            _ * updateStats(_, _, _, _, _, _)
        }
    }

    private ExecutorService executorService() {
        return Mock(ExecutorService) {
            _ * execute(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        }
    }

    private static CacheEntryEntity storedEntity() {
        return new CacheEntryEntity(
            new CacheEntryId('orders', [3] as byte[]),
            [4] as byte[],
            '5'.bytes,
            null,
            Instant.now(),
            Instant.now(),
            Instant.now().plusSeconds(20)
        )
    }
}
