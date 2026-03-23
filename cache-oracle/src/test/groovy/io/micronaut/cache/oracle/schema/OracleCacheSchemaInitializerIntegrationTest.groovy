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
import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration
import io.micronaut.cache.oracle.configuration.OracleCacheDataSourceConfiguration
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers

import java.sql.Connection

class OracleCacheSchemaInitializerIntegrationTest extends OracleIntegrationSupport {

    void initializerUsesManualSchemaExecutionPath() {
        expect:
        OracleCacheSchemaInitializer.getDeclaredMethod('initializeSchemaManually') != null
    }

    void schemaObjectsAreCreatedOnRealDatabase() {
        given:
        ApplicationContext context = newContext()

        expect:
        countUserTables(['MN_CACHE_ENTRY', 'MN_CACHE_CONFIG', 'MN_CACHE_STATS']) == 3
        hasTimezoneColumnType('MN_CACHE_ENTRY', 'EXPIRES_AT')
        hasProcedure('MN_CACHE_UPSERT_CONFIG')
        hasProcedure('MN_CACHE_UPDATE_STATS')
        hasProcedure('MN_CACHE_CLEANUP_CACHE')
        hasProcedure('MN_CACHE_REGISTER_CLEANUP_JOB')
        hasProcedure('MN_CACHE_PUT_BLOCKING')

        cleanup:
        context.close()
    }

    void schemaInitializationIsIdempotentOnRealDatabase() {
        given:
        ApplicationContext firstContext = newContext()

        when:
        firstContext.close()

        ApplicationContext secondContext = newContext()

        then:
        countUserTables(['MN_CACHE_ENTRY', 'MN_CACHE_CONFIG', 'MN_CACHE_STATS']) == 3
        hasProcedure('MN_CACHE_UPSERT_CONFIG')
        hasProcedure('MN_CACHE_UPDATE_STATS')
        hasProcedure('MN_CACHE_CLEANUP_CACHE')
        hasProcedure('MN_CACHE_REGISTER_CLEANUP_JOB')
        hasProcedure('MN_CACHE_PUT_BLOCKING')

        cleanup:
        secondContext.close()
    }

    void initializationCanRetryAfterDroppedSchemaOnRealDatabase() {
        given:
        ApplicationContext context = newContext()

        when:
        dropSchemaObjects()
        context.close()

        ApplicationContext retryContext = newContext()
        OracleCacheSchemaInitializer initializer = retryContext.getBean(OracleCacheSchemaInitializer)
        initializer.initializeSchemaManually()

        then:
        countUserTables(['MN_CACHE_ENTRY', 'MN_CACHE_CONFIG', 'MN_CACHE_STATS']) == 3
        hasProcedure('MN_CACHE_REGISTER_CLEANUP_JOB')

        cleanup:
        retryContext.close()
    }

    void cacheConfigurationsArePersistedDuringInitialization() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
            'micronaut.caches.orders.cleanup-interval': '15s',
            'micronaut.caches.orders.cleanup-batch-size': 15,
            'micronaut.caches.products.cleanup-interval': '45s',
            'micronaut.caches.products.cleanup-batch-size': 25,
            'micronaut.caches.products.expire-after-write': '1m',
        ])

        expect:
        // Initialization should materialize per-cache configuration into MN_CACHE_CONFIG rows.
        hasConfigRow('orders', 1, 15, 15)
        hasConfigRow('products', 0, 45, 25)

        cleanup:
        context.close()
    }

    void configuredCachesAreUpsertedAndCleanupJobsRegistered() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
            'micronaut.caches.orders.cleanup-interval': '15s',
            'micronaut.caches.orders.cleanup-batch-size': 15,
            'micronaut.caches.orders.maximum-size': 10,
            'micronaut.caches.orders.maximum-weight': 100,
            'micronaut.caches.products.cleanup-interval': '45s',
            'micronaut.caches.products.cleanup-batch-size': 25,
        ])

        expect:
        hasConfigRow('orders', 1, 15, 15)
        hasConfigRow('products', 0, 45, 25)
        hasSchedulerJob('MN_CACHE_CLEANUP_ORDERS', 'FREQ=SECONDLY;INTERVAL=15')
        hasSchedulerJob('MN_CACHE_CLEANUP_PRODUCTS', 'FREQ=SECONDLY;INTERVAL=45')

        cleanup:
        context.close()
    }

    void configurationBeanBindsExpectedValues() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
            'micronaut.caches.orders.lock-wait-timeout': '2s',
            'micronaut.caches.orders.cleanup-interval': '15s',
            'micronaut.caches.orders.cleanup-batch-size': 15,
            'micronaut.caches.orders.maximum-size': 10,
            'micronaut.caches.orders.maximum-weight': 100,
        ])

        when:
        OracleCacheConfiguration configuration = context.getBean(OracleCacheConfiguration, Qualifiers.byName('orders'))

        then:
        configuration.blocking
        configuration.lockWaitTimeout.seconds == 2
        configuration.cleanupInterval.seconds == 15
        configuration.cleanupBatchSize == 15
        configuration.maximumSize.asLong == 10L
        configuration.maximumWeight.asLong == 100L

        cleanup:
        context.close()
    }

    void oracleDatasourceConfigurationFailsWhenUnset() {
        given:
        ApplicationContext context = ApplicationContext.run()

        when:
        OracleCacheDataSourceConfiguration configuration = context.getBean(OracleCacheDataSourceConfiguration)
        configuration.datasource

        then:
        IllegalStateException ex = thrown()
        ex.message == 'micronaut.cache.oracle.datasource must be configured'

        cleanup:
        context.close()
    }

    void explicitDefaultDatasourceIsRecognizedAsConfigured() {
        given:
        ApplicationContext context = ApplicationContext.run([
            'micronaut.cache.oracle.datasource': 'default'
        ])

        when:
        OracleCacheDataSourceConfiguration configuration = context.getBean(OracleCacheDataSourceConfiguration)

        then:
        configuration.datasourceConfigured
        configuration.datasource == 'default'

        cleanup:
        context.close()
    }

    void multipleDatasourcesAndWrongConfiguredDatasourceFails() {
        given:
        ApplicationContext context = ApplicationContext.run([
            'micronaut.cache.oracle.datasource'   : 'missing',
            'datasources.default.url'             : oracle.jdbcUrl,
            'datasources.default.username'        : oracle.username,
            'datasources.default.password'        : oracle.password,
            'datasources.default.driver-class-name': 'oracle.jdbc.OracleDriver',
            'datasources.secondary.url'          : oracle.jdbcUrl,
            'datasources.secondary.username'     : oracle.username,
            'datasources.secondary.password'     : oracle.password,
            'datasources.secondary.driver-class-name': 'oracle.jdbc.OracleDriver',
        ])

        when:
        context.getBean(OracleCacheSchemaInitializer).initializeSchemaManually()

        then:
        IllegalStateException ex = thrown()
        ex.message == "No DataSource bean found for micronaut.cache.oracle.datasource='missing'"

        cleanup:
        context.close()
    }

    void multipleDatasourcesAndCorrectConfiguredDatasourceUsesSpecifiedDatasource() {
        given:
        ApplicationContext context = ApplicationContext.run([
            'micronaut.cache.oracle.datasource'      : 'secondary',
            'datasources.default.url'                : oracle.jdbcUrl,
            'datasources.default.username'           : oracle.username,
            'datasources.default.password'           : oracle.password,
            'datasources.default.driver-class-name'  : 'oracle.jdbc.OracleDriver',
            'datasources.secondary.url'              : oracle.jdbcUrl,
            'datasources.secondary.username'         : oracle.username,
            'datasources.secondary.password'         : oracle.password,
            'datasources.secondary.driver-class-name': 'oracle.jdbc.OracleDriver',
        ])

        when:
        context.getBean(OracleCacheSchemaInitializer).initializeSchemaManually()

        then:
        hasTableOnSelectedDatasource(context, 'secondary', 'MN_CACHE_ENTRY')
        hasTableOnSelectedDatasource(context, 'default', 'MN_CACHE_ENTRY')

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

    private boolean hasConfigRow(String cacheName, int blocking, long cleanupIntervalSeconds, long cleanupBatchSize) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('SELECT BLOCKING, CLEANUP_INTERVAL_SECONDS, CLEANUP_BATCH_SIZE FROM MN_CACHE_CONFIG WHERE CACHE_NAME = ?')) {
            statement.setString(1, cacheName)
            try (def rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false
                }
                return rs.getInt(1) == blocking && rs.getLong(2) == cleanupIntervalSeconds && rs.getLong(3) == cleanupBatchSize
            }
        }
    }

    private boolean hasTimezoneColumnType(String tableName, String columnName) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('SELECT DATA_TYPE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?')) {
            statement.setString(1, tableName)
            statement.setString(2, columnName)
            try (def rs = statement.executeQuery()) {
                return rs.next() && rs.getString(1).contains('WITH TIME ZONE')
            }
        }
    }

    private boolean hasProcedure(String objectName) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_TYPE = ? AND OBJECT_NAME = ?')) {
            statement.setString(1, 'PROCEDURE')
            statement.setString(2, objectName)
            try (def rs = statement.executeQuery()) {
                rs.next()
                return rs.getInt(1) == 1
            }
        }
    }

    private boolean hasSchedulerJob(String jobName, String repeatInterval) {
        try (Connection connection = openJdbcConnection();
             def statement = connection.prepareStatement('SELECT REPEAT_INTERVAL, ENABLED FROM USER_SCHEDULER_JOBS WHERE JOB_NAME = ?')) {
            statement.setString(1, jobName)
            try (def rs = statement.executeQuery()) {
                return rs.next() && rs.getString('REPEAT_INTERVAL') == repeatInterval && rs.getString('ENABLED') == 'TRUE'
            }
        }
    }

    private void dropSchemaObjects() {
        try (Connection connection = openJdbcConnection();
             def statement = connection.createStatement()) {
            ['MN_CACHE_REGISTER_CLEANUP_JOB', 'MN_CACHE_UPDATE_STATS', 'MN_CACHE_CLEANUP_CACHE', 'MN_CACHE_PUT_BLOCKING', 'MN_CACHE_UPSERT_CONFIG'].each {
                dropIfExists(statement, "DROP PROCEDURE ${it}")
            }
            ['MN_CACHE_ENTRY_EXPIRES_IDX', 'MN_CACHE_ENTRY_ACCESS_IDX'].each {
                dropIfExists(statement, "DROP INDEX ${it}")
            }
            ['MN_CACHE_ENTRY', 'MN_CACHE_CONFIG', 'MN_CACHE_STATS'].each {
                dropIfExists(statement, "DROP TABLE ${it}")
            }
        }
    }

    private void dropIfExists(def statement, String ddl) {
        try {
            statement.execute(ddl)
        } catch (java.sql.SQLException ignored) {
        }
    }

    private boolean hasTableOnSelectedDatasource(ApplicationContext context, String datasourceName, String tableName) {
        Environment environment = context.getBean(Environment)
        String prefix = "datasources.${datasourceName}."
        String url = environment.getProperty(prefix + 'url', String).orElse(null)
        String username = environment.getProperty(prefix + 'username', String).orElse(null)
        String password = environment.getProperty(prefix + 'password', String).orElse(null)
        try (Connection connection = java.sql.DriverManager.getConnection(url, username, password);
             def statement = connection.prepareStatement('SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?')) {
            statement.setString(1, tableName)
            try (def rs = statement.executeQuery()) {
                rs.next()
                return rs.getInt(1) == 1
            }
        } catch (java.sql.SQLException ignored) {
            return false
        }
    }
}
