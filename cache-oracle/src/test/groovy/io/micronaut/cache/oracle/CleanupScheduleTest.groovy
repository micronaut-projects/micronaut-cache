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
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository
import io.micronaut.cache.oracle.serialization.OracleKeySerializer
import io.micronaut.context.ApplicationContext
import io.micronaut.core.convert.DefaultMutableConversionService
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

class CleanupScheduleTest extends Specification {

    void cleanupRemovesEntriesExpiredByWriteTtl() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configurationWithTtlOnly(), repository, serializer(), conversionService())

        when:
        cache.runCleanup()

        then:
        1 * repository.runCleanupProcedure('orders', 100L)
    }

    void cleanupEnforcesMaxSizeAndWeight() {
        given:
        OracleCacheEntryRepository repository = Mock()
        OracleSyncCache cache = new OracleSyncCache(configurationWithLimits(), repository, serializer(), conversionService())

        when:
        cache.runCleanup()

        then:
        1 * repository.runCleanupProcedure('orders', 100L)
    }

    private static OracleCacheConfiguration configurationWithTtlOnly() {
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.orders.expire-after-write' : '1s',
                'micronaut.caches.orders.cleanup-interval'   : '365d'
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
}
