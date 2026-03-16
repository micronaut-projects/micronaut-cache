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

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.OracleContainer
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager

abstract class OracleIntegrationSupport extends Specification {

    @Shared
    protected OracleContainer oracle = OracleTestSupport.newOracleContainer()

    void setupSpec() {
        oracle.start()
        OracleTestSupport.applyCleanupJobPrivilegeGrants(oracle)
    }

    void cleanupSpec() {
        oracle.stop()
    }

    protected ApplicationContext newContext(Map<String, Object> properties = [:]) {
        return OracleTestSupport.newContext(oracle, [
            'micronaut.caches.orders.expire-after-access': '30s',
            'micronaut.caches.orders.expire-after-write' : '2m',
            'micronaut.caches.orders.record-stats'       : true,
            'micronaut.caches.orders.test-mode'          : true,
        ], properties)
    }

    protected Connection openJdbcConnection() {
        return DriverManager.getConnection(oracle.jdbcUrl, oracle.username, oracle.password)
    }
}
