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
import io.micronaut.cache.oracle.configuration.OracleCacheDataSourceConfiguration
import io.micronaut.cache.oracle.schema.OracleCacheSchemaExecutor
import io.micronaut.context.ApplicationContext
import io.micronaut.core.io.ResourceResolver
import org.testcontainers.containers.OracleContainer

import java.sql.Connection
import java.sql.DriverManager

final class OracleTckSupport {

    private static final OracleContainer ORACLE = OracleTestSupport.newOracleContainer()

    static synchronized ApplicationContext sharedContext(Map<String, Object> properties = [:]) {
        ensureStarted()
        Map<String, Object> resolved = [
            'micronaut.cache.oracle.datasource'   : 'default',
            'datasources.default.url'               : ORACLE.jdbcUrl,
            'datasources.default.username'          : ORACLE.username,
            'datasources.default.password'          : ORACLE.password,
            'datasources.default.driver-class-name' : 'oracle.jdbc.OracleDriver',
            'micronaut.caches.counter.expire-after-access' : '30s',
            'micronaut.caches.counter2.expire-after-access': '30s',
            'micronaut.caches.test.expire-after-access'    : '30s',
            'micronaut.caches.counter.test-mode'           : true,
            'micronaut.caches.counter2.test-mode'          : true,
            'micronaut.caches.test.test-mode'              : true,
        ]
        resolved.putAll(properties)
        ApplicationContext context = ApplicationContext.run(resolved)
        new OracleCacheSchemaExecutor(
            context,
            context.getBean(ResourceResolver),
            context.getBean(OracleCacheDataSourceConfiguration),
            'db/oracle-cache.sql',
            context.getBeansOfType(OracleCacheConfiguration).toList()
        ).initializeSchemaNow()
        assertSchemaReady()
        resetState()
        return context
    }

    private static synchronized void ensureStarted() {
        if (ORACLE.isRunning()) {
            return
        }
        ORACLE.start()
        OracleTestSupport.applyCleanupJobPrivilegeGrants(ORACLE)
    }

    private static void resetState() {
        try (def connection = DriverManager.getConnection(ORACLE.jdbcUrl, ORACLE.username, ORACLE.password);
             def statement = connection.createStatement()) {
            statement.executeUpdate('DELETE FROM MN_CACHE_ENTRY')
            statement.executeUpdate('DELETE FROM MN_CACHE_STATS')
        }
    }

    private static void assertSchemaReady() {
        try (Connection connection = DriverManager.getConnection(ORACLE.jdbcUrl, ORACLE.username, ORACLE.password);
             def statement = connection.prepareStatement("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'MN_CACHE_ENTRY'")) {
            try (def rs = statement.executeQuery()) {
                rs.next()
                if (rs.getInt(1) != 1) {
                    throw new IllegalStateException('Oracle TCK schema initialization did not create MN_CACHE_ENTRY')
                }
            }
        }
    }
}
