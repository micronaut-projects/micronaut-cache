/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration;
import io.micronaut.cache.oracle.configuration.OracleCacheDataSourceConfiguration;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.context.env.Environment;
import io.micronaut.core.naming.NameResolver;
import io.micronaut.inject.BeanDefinition;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;

/**
 * JDBC Executor to initialize schema and select user-supplied datasource.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
public final class OracleCacheSchemaExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCacheSchemaExecutor.class);

    private final BeanContext beanContext;
    private final OracleCacheDataSourceConfiguration dataSourceConfiguration;
    private final List<OracleCacheConfiguration> cacheConfigurations;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public OracleCacheSchemaExecutor(BeanContext beanContext,
                                     OracleCacheDataSourceConfiguration dataSourceConfiguration,
                                     List<OracleCacheConfiguration> cacheConfigurations) {
        this.beanContext = beanContext;
        this.dataSourceConfiguration = dataSourceConfiguration;
        this.cacheConfigurations = List.copyOf(cacheConfigurations);
    }

    public void initializeSchemaNow() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        JdbcConnectionSettings settings = resolveConnectionSettings();

        try {
            migrateSchema(settings);
        } catch (SQLException e) {
            initialized.set(false);
            throw new IllegalStateException("Failed to initialize Oracle cache schema", e);
        }

        try (Connection connection = settings.openConnection()) {
            initializeCacheConfigurations(connection);
        } catch (SQLException e) {
            initialized.set(false);
            throw new IllegalStateException("Failed to initialize Oracle cache schema", e);
        } catch (RuntimeException e) {
            initialized.set(false);
            throw e;
        }
    }

    private void initializeCacheConfigurations(Connection connection) throws SQLException {
        if (cacheConfigurations.isEmpty()) {
            return;
        }
        String upsertCacheConfigCall = "{ call MN_CACHE_UPSERT_CONFIG(?, ?, ?, ?, ?, ?, ?, ?) }".replace("MN", dataSourceConfiguration.getPrefix());
        String registerCleanupJobCall = "{ call MN_CACHE_REGISTER_CLEANUP_JOB(?, ?) }".replace("MN", dataSourceConfiguration.getPrefix());
        try (CallableStatement upsertConfig = connection.prepareCall(upsertCacheConfigCall);
             CallableStatement registerCleanupJob = connection.prepareCall(registerCleanupJobCall)) {
            for (OracleCacheConfiguration cacheConfiguration : cacheConfigurations) {
                OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
                String cacheName = cacheConfiguration.getCacheName();
                int blocking = cacheConfiguration.isBlocking() ? 1 : 0;
                long lockWaitTimeoutMs = cacheConfiguration.getLockWaitTimeout().toMillis();
                long cleanupIntervalSeconds = Math.max(1, cacheConfiguration.getCleanupInterval().toSeconds());
                long cleanupBatchSize = cacheConfiguration.getCleanupBatchSize();
                Long maximumSize = cacheConfiguration.getMaximumSize().isPresent() ? cacheConfiguration.getMaximumSize().getAsLong() : null;
                Long maximumWeight = cacheConfiguration.getMaximumWeight().isPresent() ? cacheConfiguration.getMaximumWeight().getAsLong() : null;

                upsertConfig.setString(1, cacheName);
                upsertConfig.setInt(2, blocking);
                upsertConfig.setLong(3, lockWaitTimeoutMs);
                upsertConfig.setLong(4, cleanupIntervalSeconds);
                upsertConfig.setLong(5, cleanupBatchSize);
                setNullableLong(upsertConfig, 6, maximumSize);
                setNullableLong(upsertConfig, 7, maximumWeight);
                upsertConfig.setObject(8, nowUtc);
                upsertConfig.execute();

                registerCleanupJob.setString(1, cacheName);
                registerCleanupJob.setLong(2, cleanupIntervalSeconds);
                registerCleanupJob.execute();
            }
        }
    }

    private void setNullableLong(CallableStatement callableStatement, int index, Long value) throws SQLException {
        if (value == null) {
            callableStatement.setNull(index, Types.NUMERIC);
        } else {
            callableStatement.setLong(index, value);
        }
    }

    private void migrateSchema(JdbcConnectionSettings settings) throws SQLException {
        String prefix = dataSourceConfiguration.getPrefix().toUpperCase(Locale.ROOT);
        try (OracleFlywayDataSource dataSource = new OracleFlywayDataSource(settings)) {
            boolean baselineOnMigrate;
            try (Connection connection = settings.openConnection()) {
                baselineOnMigrate = shouldBaselineHistory(connection, prefix);
            }
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

    private boolean shouldBaselineHistory(Connection connection, String prefix) {
        String historyTable = "FLYWAY_SCHEMA_HISTORY_" + prefix;
        try {
            if (objectExists(connection, "USER_TABLES", "TABLE_NAME", historyTable)) {
                return false;
            }
            return objectExists(connection, "USER_OBJECTS", "OBJECT_NAME", "%");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to determine Flyway baseline state for Oracle cache", e);
        }
    }

    private boolean objectExists(Connection connection, String table, String column, String pattern) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " LIKE ? ESCAPE '\\'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private JdbcConnectionSettings resolveConnectionSettings() {
        Collection<String> availableDataSourceNames = beanContext.getBeanDefinitions(DataSource.class)
            .stream()
            .map(this::resolveDataSourceName)
            .toList();
        Collection<String> displayNames = beanContext.getBeanDefinitions(DataSource.class)
            .stream()
            .map(this::describeDataSourceBean)
            .toList();
        if (availableDataSourceNames.isEmpty()) {
            throw new IllegalStateException("No DataSource beans available for Oracle cache initialization");
        }
        String dataSourceName = dataSourceConfiguration.getDatasource();

        if (!availableDataSourceNames.contains(dataSourceName)) {
            LOG.error("Configured Oracle cache datasource '{}' was not found. Available datasource beans: {}", dataSourceName, displayNames);
            throw new IllegalStateException(
                "No DataSource bean found for micronaut.oracle.cache.datasource='" + dataSourceName + "'"
            );
        }
        LOG.info("Using DataSource '{}' for Oracle cache initialization", dataSourceName);

        Environment environment = beanContext.getBean(Environment.class);
        String prefix = "datasources." + dataSourceName + ".";
        String url = environment.getProperty(prefix + "url", String.class).orElseThrow(() ->
            new IllegalStateException("No datasource URL configured for '" + dataSourceName + "'")
        );
        String username = environment.getProperty(prefix + "username", String.class).orElse(null);
        String password = environment.getProperty(prefix + "password", String.class).orElse(null);
        String driverClassName = environment.getProperty(prefix + "driver-class-name", String.class)
            .or(() -> environment.getProperty(prefix + "driverClassName", String.class))
            .orElse(null);
        if (driverClassName != null && !driverClassName.isBlank()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Configured datasource driver class not found for '" + dataSourceName + "': " + driverClassName, e);
            }
        }
        return new JdbcConnectionSettings(url, username, password, driverClassName);
    }

    private record JdbcConnectionSettings(String url,
                                          String username,
                                          String password,
                                          String driverClassName) {
        private Connection openConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(url, username, password);
        }
    }

    private static final class OracleFlywayDataSource implements DataSource, AutoCloseable {
        private final JdbcConnectionSettings settings;

        private OracleFlywayDataSource(JdbcConnectionSettings settings) {
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

    private String describeDataSourceBean(BeanDefinition<DataSource> definition) {
        String candidate = definition.stringValue(AnnotationUtil.NAMED).orElse(null);
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        if (definition.getDeclaredQualifier() != null) {
            return definition.getDeclaredQualifier().toString();
        }
        return definition.getBeanType().getName();
    }

    private String resolveDataSourceName(BeanDefinition<DataSource> definition) {
        if (definition instanceof NameResolver nameResolver) {
            String resolvedName = nameResolver.resolveName().orElse(null);
            if (resolvedName != null && !resolvedName.isBlank()) {
                return resolvedName;
            }
        }
        String candidate = definition.stringValue(AnnotationUtil.NAMED).orElse(null);
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        if (definition.getDeclaredQualifier() != null) {
            String qualifier = definition.getDeclaredQualifier().toString();
            if (qualifier.startsWith("@Named('") && qualifier.endsWith("')")) {
                return qualifier.substring(8, qualifier.length() - 2);
            }
        }
        return definition.getBeanType().getName();
    }
}
