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

import io.micronaut.data.connection.annotation.Connectable
import spock.lang.Specification

import javax.sql.DataSource
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

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource)

        when:
        initializer.onApplicationEvent(null)

        then:
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_ENTRY') }
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_CONFIG') }
        executedStatements.any { it.contains('CREATE TABLE MN_CACHE_STATS') }
        executedStatements.any { it.contains('CREATE INDEX MN_CACHE_ENTRY_EXPIRES_IDX') }
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
                throw new SQLException('name is already used by an existing object', '42000', 955)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource)

        when:
        initializer.onApplicationEvent(null)
        initializer.onApplicationEvent(null)

        then:
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
                throw new SQLException('transient error', '42000', 900)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource)

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
                throw new SQLException('insufficient privileges', '42000', 1031)
            }
            true
        }

        OracleCacheSchemaInitializer initializer = new OracleCacheSchemaInitializer(dataSource)

        when:
        initializer.onApplicationEvent(null)

        then:
        IllegalStateException ex = thrown()
        ex.cause instanceof SQLException
        ((SQLException) ex.cause).errorCode == 1031
    }
}
