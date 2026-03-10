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

import io.micronaut.data.connection.annotation.Connectable;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initializes Oracle cache schema from a single SQL resource.
 */
@Singleton
@Connectable
public class OracleCacheSchemaInitializer implements ApplicationEventListener<ServerStartupEvent> {

    private static final String DEFAULT_RESOURCE_PATH = "db/oracle-cache.sql";

    private final DataSource dataSource;
    private final String scriptResourcePath;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public OracleCacheSchemaInitializer(DataSource dataSource) {
        this(dataSource, DEFAULT_RESOURCE_PATH);
    }

    OracleCacheSchemaInitializer(DataSource dataSource, String scriptResourcePath) {
        this.dataSource = dataSource;
        this.scriptResourcePath = scriptResourcePath;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
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
        } catch (SQLException e) {
            initialized.set(false);
            throw new IllegalStateException("Failed to initialize Oracle cache schema", e);
        } catch (RuntimeException e) {
            initialized.set(false);
            throw e;
        }
    }

    private List<String> readStatements() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptResourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Schema SQL resource not found: " + scriptResourcePath);
            }
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
