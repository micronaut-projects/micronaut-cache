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
package io.micronaut.cache.oracle.schema

import io.micronaut.cache.oracle.OracleIntegrationSupport
import io.micronaut.context.ApplicationContext
import java.sql.Connection

class OracleCacheSchemaInitializerIntegrationTest extends OracleIntegrationSupport {

    void schemaInitializationIsIdempotentOnRealDatabase() {
        given:
        ApplicationContext context = newContext()
        OracleCacheSchemaInitializer initializer = context.getBean(OracleCacheSchemaInitializer)

        when:
        initializer.onApplicationEvent(null)
        initializer.onApplicationEvent(null)

        then:
        countUserTables(['MN_CACHE_ENTRY', 'MN_CACHE_CONFIG', 'MN_CACHE_STATS']) == 3

        cleanup:
        context.close()
    }

    void cacheConfigurationsArePersistedDuringInitialization() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
            'micronaut.caches.orders.cleanup-interval': '15s',
            'micronaut.caches.products.cleanup-interval': '45s',
            'micronaut.caches.products.expire-after-write': '1m',
        ])

        expect:
        hasConfigRow('orders', 1, 15)
        hasConfigRow('products', 0, 45)

        cleanup:
        context.close()
    }

    private int countUserTables(List<String> tableNames) {
        String inClause = tableNames.collect { "'${it}'" }.join(',')
        try (Connection connection = openJdbcConnection();
             def statement = connection.createStatement();
             def rs = statement.executeQuery("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN (${inClause})")) {
            rs.next()
            return rs.getInt(1)
        }
    }

    private boolean hasConfigRow(String cacheName, int blocking, long cleanupIntervalSeconds) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('SELECT BLOCKING, CLEANUP_INTERVAL_SECONDS FROM MN_CACHE_CONFIG WHERE CACHE_NAME = ?')) {
            statement.setString(1, cacheName)
            try (def rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false
                }
                return rs.getInt(1) == blocking && rs.getLong(2) == cleanupIntervalSeconds
            }
        }
    }
}
