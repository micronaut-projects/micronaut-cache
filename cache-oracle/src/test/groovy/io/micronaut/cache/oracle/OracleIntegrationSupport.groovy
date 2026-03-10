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

import io.micronaut.cache.oracle.schema.OracleCacheSchemaInitializer
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.Container
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.MountableFile
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager

abstract class OracleIntegrationSupport extends Specification {

    /**
     * Integration tests are designed to fail explicitly when Docker/Testcontainers is unavailable.
     *
     * Image override order (first non-blank wins):
     * 1) -Doracle.test.image=...
     * 2) ORACLE_TEST_IMAGE=...
     * 3) -Doracle.container.image=... (Testcontainers standard)
     * 4) TESTCONTAINERS_ORACLE_CONTAINER_IMAGE=... (Testcontainers standard)
     * 5) default ghcr.io/gvenzl/oracle-free:latest-faststart
     */
    private static final String DEFAULT_IMAGE = 'ghcr.io/gvenzl/oracle-free:latest-faststart'
    private static final String IMAGE_OVERRIDE_PROPERTY = 'oracle.test.image'
    private static final String IMAGE_OVERRIDE_ENV = 'ORACLE_TEST_IMAGE'
    private static final String TESTCONTAINERS_IMAGE_PROPERTY = 'oracle.container.image'
    private static final String TESTCONTAINERS_IMAGE_ENV = 'TESTCONTAINERS_ORACLE_CONTAINER_IMAGE'
    private static final String CLEANUP_JOB_GRANTS_SCRIPT_CLASSPATH = 'oracle-init/grant-cleanup-job-privileges.sql'
    private static final String CLEANUP_JOB_GRANTS_SCRIPT_CONTAINER_PATH = '/tmp/grant-cleanup-job-privileges.sql'
    private static final String USERNAME = 'TEST_USER'
    private static final String PASSWORD = 'test_password'
    private static final String ADMIN_PASSWORD = 'AdminPwd123'
    private static final String DATABASE_NAME = 'TESTPDB'

    @Shared
    protected OracleContainer oracle = new OracleContainer(
        DockerImageName.parse(resolveOracleImage())
            .asCompatibleSubstituteFor('gvenzl/oracle-xe')
    )
        .withDatabaseName(DATABASE_NAME)
        .withEnv('ORACLE_PASSWORD', ADMIN_PASSWORD)
        .withEnv('APP_USER', USERNAME)
        .withEnv('APP_USER_PASSWORD', PASSWORD)
        .withUsername(USERNAME)
        .withPassword(PASSWORD)
        .withCopyFileToContainer(
            MountableFile.forClasspathResource(CLEANUP_JOB_GRANTS_SCRIPT_CLASSPATH),
            CLEANUP_JOB_GRANTS_SCRIPT_CONTAINER_PATH
        )

    void setupSpec() {
        oracle.start()
        applyCleanupJobPrivilegeGrants()
    }

    void cleanupSpec() {
        oracle.stop()
    }

    protected ApplicationContext newContext(Map<String, Object> properties = [:]) {
        Map<String, Object> base = [
            'datasources.default.url'               : oracle.jdbcUrl,
            'datasources.default.username'          : oracle.username,
            'datasources.default.password'          : oracle.password,
            'datasources.default.driver-class-name' : 'oracle.jdbc.OracleDriver',
            'micronaut.caches.orders.expire-after-access': '30s',
            'micronaut.caches.orders.expire-after-write' : '2m',
            'micronaut.caches.orders.record-stats'       : true,
            'micronaut.caches.orders.test-mode'          : true,
        ]
        base.putAll(properties)
        ApplicationContext context = ApplicationContext.run(base)
        context.getBean(OracleCacheSchemaInitializer).onApplicationEvent(null)
        return context
    }

    private static String resolveOracleImage() {
        String image = firstNonBlank(
            System.getProperty(IMAGE_OVERRIDE_PROPERTY),
            System.getenv(IMAGE_OVERRIDE_ENV),
            System.getProperty(TESTCONTAINERS_IMAGE_PROPERTY),
            System.getenv(TESTCONTAINERS_IMAGE_ENV)
        )
        if (image != null) {
            return image
        }
        return DEFAULT_IMAGE
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate
            }
        }
        return null
    }

    protected Connection openJdbcConnection() {
        return DriverManager.getConnection(oracle.jdbcUrl, oracle.username, oracle.password)
    }

    private void applyCleanupJobPrivilegeGrants() {
        Container.ExecResult result = oracle.execInContainer(
            'bash',
            '-lc',
            "sqlplus -s / as sysdba @${CLEANUP_JOB_GRANTS_SCRIPT_CONTAINER_PATH}"
        )
        if (result.exitCode != 0) {
            throw new IllegalStateException(
                "Failed applying cleanup job privilege grants (exit=${result.exitCode}). stdout=${result.stdout} stderr=${result.stderr}"
            )
        }
    }
}
