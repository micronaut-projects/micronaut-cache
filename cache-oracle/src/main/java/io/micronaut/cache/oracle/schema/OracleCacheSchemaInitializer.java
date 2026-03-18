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
import io.micronaut.data.connection.annotation.Connectable;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initializes Oracle cache schema from a single SQL resource.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
@Singleton
@Connectable
public class OracleCacheSchemaInitializer implements ApplicationEventListener<ApplicationStartupEvent> {

    private static final String DEFAULT_RESOURCE_PATH = "db/oracle-cache.sql";
    private static final String UPSERT_CACHE_CONFIG_CALL = "{ call MN_CACHE_UPSERT_CONFIG(?, ?, ?, ?, ?, ?, ?, ?) }";
    private static final String REGISTER_CLEANUP_JOB_CALL = "{ call MN_CACHE_REGISTER_CLEANUP_JOB(?, ?) }";

    private final DataSource dataSource;
    private final ResourceResolver resourceResolver;
    private final List<OracleCacheConfiguration> cacheConfigurations;
    private final String scriptResourcePath;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public OracleCacheSchemaInitializer(DataSource dataSource,
                                        ResourceResolver resourceResolver,
                                        List<OracleCacheConfiguration> cacheConfigurations) {
        this(dataSource, resourceResolver, DEFAULT_RESOURCE_PATH, cacheConfigurations);
    }

    OracleCacheSchemaInitializer(DataSource dataSource,
                                 ResourceResolver resourceResolver,
                                 String scriptResourcePath,
                                 List<OracleCacheConfiguration> cacheConfigurations) {
        this.dataSource = dataSource;
        this.resourceResolver = resourceResolver;
        this.scriptResourcePath = scriptResourcePath;
        this.cacheConfigurations = List.copyOf(cacheConfigurations);
    }

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        initializeSchema();
    }

    private void initializeSchema() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        List<String> statements = readStatements();
        try (Connection connection = dataSource.getConnection();
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
        try (InputStream stream = resourceResolver.getResource("classpath:" + scriptResourcePath).orElseThrow(() ->
            new IllegalStateException("Schema SQL resource not found: " + scriptResourcePath)
        ).openStream()) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return parseStatements(stripLineComments(content));
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
}
