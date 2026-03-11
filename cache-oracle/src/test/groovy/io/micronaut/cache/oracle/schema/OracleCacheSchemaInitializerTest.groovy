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

import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration
import io.micronaut.context.ApplicationContext
import io.micronaut.data.connection.annotation.Connectable
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

class OracleCacheSchemaInitializerTest extends Specification {

    void initializerRunsWithinConnectableContext() {
        expect:
        OracleCacheSchemaInitializer.getAnnotation(Connectable) != null
    }

    void tablesAreCreated() {
        given:
        List<String> executedStatements = []
        DataSource dataSource = Mock()
        Connection connection = Mock()
        Statement statement = Mock()

        dataSource.getConnection() >> connection
        connection.createStatement() >> statement
        statement.execute(_ as String) >> { String sql ->
            executedStatements.add(sql)
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource, [])

        when:
        initializer.onApplicationEvent(null)

        then:
        // Verify we emit the expected DDL and procedural objects, not just a subset of schema artifacts.
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_ENTRY') }
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_CONFIG') }
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_STATS') }
        executedStatements.any { it.contains('CREATE INDEX MN_CACHE_ENTRY_EXPIRES_IDX') }
        executedStatements.any { it.contains('CREATE OR REPLACE PROCEDURE MN_CACHE_UPSERT_CONFIG') }
        executedStatements.any { it.contains('CREATE OR REPLACE PROCEDURE MN_CACHE_CLEANUP_CACHE') }
        executedStatements.any { it.contains('CREATE OR REPLACE PROCEDURE MN_CACHE_REGISTER_CLEANUP_JOB') }
        executedStatements.any { it.contains('CREATE OR REPLACE PROCEDURE MN_CACHE_PUT_BLOCKING') }
    }

    void bootstrapIsIdempotent() {
        given:
        DataSource dataSource = Mock()
        Connection connection = Mock()
        Statement statement = Mock()
        statement.execute(_ as String) >> { String sql ->
            if (sql.startsWith('CREATE')) {
                // ORA-00955 equivalent: object already exists during repeated bootstrap.
                throw new SQLException('name is already used by an existing object', '42000', 955)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource, [])

        when:
        initializer.onApplicationEvent(null)
        initializer.onApplicationEvent(null)

        then:
        // Idempotency means startup can retry initialization without breaking subsequent runs.
        noExceptionThrown()
        1 * dataSource.getConnection() >> connection
        1 * connection.createStatement() >> statement
    }

    void initializationCanRetryAfterFailure() {
        given:
        DataSource dataSource = Mock()
        Connection connection = Mock()
        Statement statement = Mock()
        boolean firstCall = true

        dataSource.getConnection() >> connection
        connection.createStatement() >> statement
        statement.execute(_ as String) >> {
            if (firstCall) {
                firstCall = false
                // Simulate transient startup fault: first call fails, second should recover.
                throw new SQLException('transient error', '42000', 900)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource, [])

        when:
        initializer.onApplicationEvent(null)

        then:
        thrown(IllegalStateException)

        when:
        initializer.onApplicationEvent(null)

        then:
        noExceptionThrown()
    }

    void privilegeErrorIsSurfacedClearly() {
        given:
        DataSource dataSource = Mock()
        Connection connection = Mock()
        Statement statement = Mock()

        dataSource.getConnection() >> connection
        connection.createStatement() >> statement
        statement.execute(_ as String) >> { String sql ->
            if (sql.contains('MN_CACHE_REGISTER_CLEANUP_JOB')) {
                // ORA-01031 simulation: cleanup job registration missing privileges.
                throw new SQLException('insufficient privileges', '42000', 1031)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource, [])

        when:
        initializer.onApplicationEvent(null)

        then:
        IllegalStateException ex = thrown()
        ex.cause instanceof SQLException
        ((SQLException) ex.cause).errorCode == 1031
    }

    void configuredCachesAreUpsertedAndCleanupJobsRegistered() {
        given:
        DataSource dataSource = Mock()
        Connection connection = Mock()
        Statement statement = Mock()
        CallableStatement upsert = Mock()
        CallableStatement register = Mock()
        OracleCacheConfiguration orders = configuration('orders', [
                'blocking'         : true,
                'lock-wait-timeout': '2s',
                'cleanup-interval' : '15s',
                'cleanup-batch-size': 15,
                'maximum-size'     : 10,
                'maximum-weight'   : 100
        ])
        OracleCacheConfiguration products = configuration('products', [
                'cleanup-interval': '45s',
                'cleanup-batch-size': 25
        ])

        dataSource.getConnection() >> connection
        connection.createStatement() >> statement
        statement.execute(_ as String) >> true
        connection.prepareCall(_ as String) >> { String sql ->
            if (sql.contains('MN_CACHE_UPSERT_CONFIG')) {
                return upsert
            }
            return register
        }
        upsert.execute() >> true
        register.execute() >> true

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource, [orders, products])

        when:
        initializer.onApplicationEvent(null)

        then:
        // Each cache config should upsert once and schedule one cleanup job with its own interval.
        2 * upsert.execute()
        1 * upsert.setLong(5, 15L)
        1 * upsert.setLong(5, 25L)
        1 * register.setString(1, 'orders')
        1 * register.setLong(2, 15L)
        1 * register.setString(1, 'products')
        1 * register.setLong(2, 45L)
        2 * register.execute()
    }

    private static OracleCacheConfiguration configuration(String cacheName, Map<String, Object> values) {
        Map<String, Object> properties = values.collectEntries { String key, Object value ->
            [(("micronaut.caches.${cacheName}.${key}").toString()): value]
        }
        ApplicationContext context = ApplicationContext.run(properties)
        try {
            return context.getBean(OracleCacheConfiguration, Qualifiers.byName(cacheName))
        } finally {
            context.close()
        }
    }
}
