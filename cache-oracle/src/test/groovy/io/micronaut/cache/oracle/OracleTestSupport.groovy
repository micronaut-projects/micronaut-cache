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
import io.micronaut.cache.oracle.schema.OracleCacheSchemaInitializer
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.Container
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

final class OracleTestSupport {

    static final String DEFAULT_IMAGE = 'ghcr.io/gvenzl/oracle-free:23.5-faststart'
    static final String IMAGE_OVERRIDE_PROPERTY = 'oracle.test.image'
    static final String IMAGE_OVERRIDE_ENV = 'ORACLE_TEST_IMAGE'
    static final String TESTCONTAINERS_IMAGE_PROPERTY = 'oracle.container.image'
    static final String TESTCONTAINERS_IMAGE_ENV = 'TESTCONTAINERS_ORACLE_CONTAINER_IMAGE'
    static final String CLEANUP_JOB_GRANTS_SCRIPT_CLASSPATH = 'oracle-init/grant-cleanup-job-privileges.sql'
    static final String CLEANUP_JOB_GRANTS_SCRIPT_CONTAINER_PATH = '/tmp/grant-cleanup-job-privileges.sql'
    static final String USERNAME = 'TEST_USER'
    static final String PASSWORD = 'test_password'
    static final String ADMIN_PASSWORD = 'AdminPwd123'
    static final String DATABASE_NAME = 'TESTPDB'

    static OracleContainer newOracleContainer() {
        return new OracleContainer(
            DockerImageName.parse(resolveOracleImage()).asCompatibleSubstituteFor('gvenzl/oracle-xe')
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
    }

    static void applyCleanupJobPrivilegeGrants(OracleContainer oracle) {
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

    static ApplicationContext newContext(OracleContainer oracle,
                                         Map<String, Object> baseProperties,
                                         Map<String, Object> properties = [:]) {
        Map<String, Object> resolved = [
            'micronaut.cache.oracle.datasource'   : 'default',
            'micronaut.cache.oracle.prefix'       : 'MN',
            'datasources.default.url'               : oracle.jdbcUrl,
            'datasources.default.username'          : oracle.username,
            'datasources.default.password'          : oracle.password,
            'datasources.default.driver-class-name' : 'oracle.jdbc.OracleDriver',
        ]
        resolved.putAll(baseProperties)
        resolved.putAll(properties)
        ApplicationContext context = ApplicationContext.run(resolved)
        new OracleCacheSchemaExecutor(
            context,
            context.getBean(OracleCacheDataSourceConfiguration),
            context.getBeansOfType(OracleCacheConfiguration).toList()
        ).initializeSchemaNow()
        return context
    }

    static String resolveOracleImage() {
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

    static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate
            }
        }
        return null
    }
}
