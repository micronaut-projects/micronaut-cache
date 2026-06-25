/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.oracle.schema;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.util.Map;

@Singleton
@Requires(classes = Flyway.class)
final class OracleCacheFlywaySchemaMigrator implements OracleCacheSchemaMigrator {
    @Override
    public void migrate(OracleCacheSchemaExecutor.JdbcConnectionSettings settings,
                        String prefix,
                        boolean baselineOnMigrate) throws SQLException {
        try (OracleFlywayDataSource dataSource = new OracleFlywayDataSource(settings)) {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/test-migration/oracle-cache", "classpath:db/migration/oracle-cache")
                .table("FLYWAY_SCHEMA_HISTORY_" + prefix)
                .placeholders(Map.of("cachePrefix", prefix))
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion("0")
                .load();
            flyway.migrate();
        }
    }

    private static final class OracleFlywayDataSource implements DataSource, AutoCloseable {
        private final OracleCacheSchemaExecutor.JdbcConnectionSettings settings;

        private OracleFlywayDataSource(OracleCacheSchemaExecutor.JdbcConnectionSettings settings) {
            this.settings = settings;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return settings.openConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return java.sql.DriverManager.getConnection(settings.url(), username, password);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }

        @Override
        public ConnectionBuilder createConnectionBuilder() throws SQLException {
            throw new SQLException("ConnectionBuilder not supported");
        }

        @Override
        public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
            throw new SQLException("ShardingKeyBuilder not supported");
        }

        @Override
        public void close() {
        }
    }
}
