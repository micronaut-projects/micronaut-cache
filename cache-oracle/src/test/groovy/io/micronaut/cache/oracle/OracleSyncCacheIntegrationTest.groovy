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

import io.micronaut.cache.oracle.persistence.CacheStatsEntity
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.inject.BeanDefinitionReference
import io.micronaut.inject.QualifiedBeanType
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.serde.annotation.Serdeable

import java.sql.Connection
import java.time.OffsetDateTime
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger

class OracleSyncCacheIntegrationTest extends OracleIntegrationSupport {

    @Serdeable
    static class TestCar {
        String model
        int year
    }

    void putGetAndInvalidateUseRealOracleTable() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        // Integration intent: verify we persist/fetch/invalidate against Oracle tables, not mocks.
        cache.put('car:model:s3', 101)
        Optional<Integer> stored = cache.get('car:model:s3', Argument.of(Integer))
        cache.invalidate('car:model:s3')
        Optional<Integer> afterInvalidate = cache.get('car:model:s3', Argument.of(Integer))

        then:
        stored.present
        stored.get() == 101
        afterInvalidate.empty

        cleanup:
        context.close()
    }

    void cacheOperationsWorkWhenFlywayMigratorIsUnavailableAndSchemaExists() {
        given:
        ApplicationContext schemaContext = newContext()
        schemaContext.close()

        ApplicationContext context = newContextWithoutFlywayMigrator()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        cache.put('car:model:external-schema', 303)
        Optional<Integer> stored = cache.get('car:model:external-schema', Argument.of(Integer))
        cache.invalidate('car:model:external-schema')
        Optional<Integer> afterInvalidate = cache.get('car:model:external-schema', Argument.of(Integer))

        then:
        context.findBean(io.micronaut.cache.oracle.schema.OracleCacheSchemaInitializer).empty
        stored.present
        stored.get() == 303
        afterInvalidate.empty

        cleanup:
        context.close()
    }

    void cacheOperationsUseConfiguredPrefixTables() {
        given:
        ApplicationContext context = newContext([
            'micronaut.oracle.cache.prefix': 'ALT'
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        cache.put('car:model:prefixed', 202)
        Optional<Integer> stored = cache.get('car:model:prefixed', Argument.of(Integer))
        cache.invalidate('car:model:prefixed')
        Optional<Integer> afterInvalidate = cache.get('car:model:prefixed', Argument.of(Integer))

        then:
        stored.present
        stored.get() == 202
        afterInvalidate.empty
        tableRowCount('ALT_CACHE_ENTRY') == 0
        tableRowCount('MN_CACHE_ENTRY') == 0

        cleanup:
        context.close()
    }

    void putIfAbsentReturnsExistingValueFromDatabase() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        // First write seeds the DB row; second call must keep existing value intact.
        cache.put('car:model:x', 7)
        Optional<Integer> duplicate = cache.putIfAbsent('car:model:x', 9)

        then:
        duplicate.present
        duplicate.get() == 7

        cleanup:
        context.close()
    }

    void putGetRoundTripsPojoValueAsJson() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        // Regression for JSON value serialization: object payloads must round-trip through Oracle storage.
        cache.put('car:model:json', new TestCar(model: 'Hello', year: 2026))
        Optional<TestCar> stored = cache.get('car:model:json', Argument.of(TestCar))

        then:
        stored.present
        stored.get().model == 'Hello'
        stored.get().year == 2026

        cleanup:
        context.close()
    }

    void blockingModeReadsPersistedValueWithoutRecomputing() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        AtomicInteger supplierCalls = new AtomicInteger(0)

        when:
        // First call computes and persists.
        Integer first = cache.get('car:model:blocking', Argument.of(Integer), {
            supplierCalls.incrementAndGet()
            return 42
        })
        // Second call should read cached value without invoking supplier again.
        Integer second = cache.get('car:model:blocking', Argument.of(Integer), {
            supplierCalls.incrementAndGet()
            return 84
        })

        then:
        first == 42
        second == 42
        supplierCalls.get() == 1

        cleanup:
        context.close()
    }

    void statsAreUpdatedAsynchronouslyFromCacheOperations() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        cache.put('stats:key', 21)
        cache.get('stats:key', Argument.of(Integer))
        cache.get('stats:missing', Argument.of(Integer))
        cache.invalidate('stats:key')

        then:
        CacheStatsEntity stats = waitForStats('orders') {
            Optional<CacheStatsEntity> row = readStats('orders')
            row.present && row.get().putCount >= 1 && row.get().hitCount >= 1 && row.get().missCount >= 1 && row.get().invalidateCount >= 1
        }
        stats.putCount >= 1
        stats.hitCount >= 1
        stats.missCount >= 1
        stats.invalidateCount >= 1

        cleanup:
        context.close()
    }

    private CacheStatsEntity waitForStats(String cacheName,
                                                  Closure<Boolean> ready) {
        long deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline) {
            Optional<CacheStatsEntity> stats = readStats(cacheName)
            if (stats.present && ready.call()) {
                return stats.get()
            }
            Thread.sleep(100L)
        }
        return readStats(cacheName).orElseThrow()
    }

    private Optional<CacheStatsEntity> readStats(String cacheName) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('''
                 SELECT CACHE_NAME, HIT_COUNT, MISS_COUNT, PUT_COUNT, INVALIDATE_COUNT, UPDATED_AT
                 FROM MN_CACHE_STATS
                 WHERE CACHE_NAME = ?
             ''')) {
            statement.setString(1, cacheName)
            try (def rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty()
                }
                return Optional.of(new CacheStatsEntity(
                    rs.getString('CACHE_NAME'),
                    rs.getLong('HIT_COUNT'),
                    rs.getLong('MISS_COUNT'),
                    rs.getLong('PUT_COUNT'),
                    rs.getLong('INVALIDATE_COUNT'),
                    rs.getObject('UPDATED_AT', OffsetDateTime).toInstant()
                ))
            }
        }
    }

    private long tableRowCount(String tableName) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.createStatement();
             def rs = statement.executeQuery("SELECT COUNT(*) FROM ${tableName}")) {
            rs.next()
            return rs.getLong(1)
        }
    }

    private ApplicationContext newContextWithoutFlywayMigrator(Map<String, Object> properties = [:]) {
        Map<String, Object> resolved = [
            'micronaut.oracle.cache.datasource'   : 'default',
            'micronaut.oracle.cache.prefix'       : 'MN',
            'datasources.default.url'             : oracle.jdbcUrl,
            'datasources.default.username'        : oracle.username,
            'datasources.default.password'        : oracle.password,
            'datasources.default.driverClassName' : 'oracle.jdbc.OracleDriver',
            'datasources.default.dialect'         : 'ORACLE',
            'micronaut.caches.orders.expire-after-write': '30s',
            'micronaut.caches.orders.record-stats': true
        ]
        resolved.putAll(properties)
        return ApplicationContext.builder()
            .properties(resolved)
            .beansPredicate({ QualifiedBeanType<?> beanType -> !isFlywayMigrator(beanType) })
            .start()
    }

    private static boolean isFlywayMigrator(QualifiedBeanType<?> beanType) {
        beanType instanceof BeanDefinitionReference<?> && beanType.beanDefinitionName.contains('OracleCacheFlywaySchemaMigrator')
    }
}
