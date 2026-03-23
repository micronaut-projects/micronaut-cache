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
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.inject.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private static final String UPSERT_CACHE_CONFIG_CALL = "{ call MN_CACHE_UPSERT_CONFIG(?, ?, ?, ?, ?, ?, ?, ?) }";
    private static final String REGISTER_CLEANUP_JOB_CALL = "{ call MN_CACHE_REGISTER_CLEANUP_JOB(?, ?) }";

    private final BeanContext beanContext;
    private final ResourceResolver resourceResolver;
    private final OracleCacheDataSourceConfiguration dataSourceConfiguration;
    private final List<OracleCacheConfiguration> cacheConfigurations;
    private final String scriptResourcePath;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public OracleCacheSchemaExecutor(BeanContext beanContext,
                                     ResourceResolver resourceResolver,
                                     OracleCacheDataSourceConfiguration dataSourceConfiguration,
                                     String scriptResourcePath,
                                     List<OracleCacheConfiguration> cacheConfigurations) {
        this.beanContext = beanContext;
        this.resourceResolver = resourceResolver;
        this.dataSourceConfiguration = dataSourceConfiguration;
        this.scriptResourcePath = scriptResourcePath;
        this.cacheConfigurations = List.copyOf(cacheConfigurations);
    }

    public void initializeSchemaNow() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        List<String> statements = readStatements();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                try {
                    statement.execute(sql);
                } catch (SQLException e) {
                    if (!isIgnorableExistsError(e)) {
                        throw new IllegalStateException("Failed to initialize Oracle cache schema", e);
                    }
                }
            }
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

        try (CallableStatement upsertConfig = connection.prepareCall(UPSERT_CACHE_CONFIG_CALL);
             CallableStatement registerCleanupJob = connection.prepareCall(REGISTER_CLEANUP_JOB_CALL)) {
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

    private List<String> readStatements() {
        try (InputStream stream = resourceResolver.getResource("classpath:" + scriptResourcePath).get().openStream()) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return parseStatements(stripLineComments(content));
        } catch (java.util.NoSuchElementException e) {
            throw new IllegalStateException("Schema SQL resource not found: " + scriptResourcePath, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema SQL resource: " + scriptResourcePath, e);
        }
    }

    private String stripLineComments(String content) {
        StringBuilder filtered = new StringBuilder(content.length());
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (!line.trim().startsWith("--")) {
                filtered.append(line).append('\n');
            }
        }
        return filtered.toString();
    }

    private List<String> parseStatements(String content) {
        String[] lines = content.split("\\r?\\n");
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder(content.length());
        boolean plsqlBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (!plsqlBlock && (startsWithIgnoreCase(trimmed, "CREATE OR REPLACE PROCEDURE") || startsWithIgnoreCase(trimmed, "BEGIN"))) {
                plsqlBlock = true;
            }

            if (plsqlBlock && "/".equals(trimmed)) {
                String sql = current.toString().trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                current.setLength(0);
                plsqlBlock = false;
                continue;
            }

            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(line);

            if (!plsqlBlock && trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                if (!sql.isEmpty()) {
                    statements.add(trimTrailingSemicolon(sql));
                }
                current.setLength(0);
            }
        }

        String trailing = current.toString().trim();
        if (!trailing.isEmpty()) {
            statements.add(trimTrailingSemicolon(trailing));
        }
        return statements;
    }

    private String trimTrailingSemicolon(String sql) {
        if (sql.endsWith(";")) {
            return sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private boolean isIgnorableExistsError(SQLException e) {
        return e.getErrorCode() == 955;
    }

    private Connection openConnection() throws SQLException {
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
                "No DataSource bean found for micronaut.cache.oracle.datasource='" + dataSourceName + "'"
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
        return DriverManager.getConnection(url, username, password);
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
