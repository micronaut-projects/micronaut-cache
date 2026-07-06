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
package io.micronaut.cache.oracle.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global datasource selection for the Oracle cache module.
 *
 * @author Davide Cocco
 * @since 6.2.0
 */
@ConfigurationProperties("micronaut.oracle.cache")
public final class OracleCacheDataSourceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(OracleCacheDataSourceConfiguration.class);

    /**
     * Default SQL object prefix.
     */
    public static final String DEFAULT_PREFIX = "MN";

    private String datasource;
    private String prefix = DEFAULT_PREFIX;

    /**
     * Gets the configured datasource name.
     *
     * @return The datasource name used by the Oracle cache
     */
    public String getDatasource() {
        if (datasource == null || datasource.isBlank()) {
            throw new IllegalStateException("micronaut.oracle.cache.datasource must be configured");
        }
        return datasource;
    }

    /**
     * Checks whether the datasource name is configured.
     *
     * @return Whether the Oracle cache datasource name is configured
     */
    public boolean isDatasourceConfigured() {
        return StringUtils.hasText(datasource);
    }

    /**
     * Sets the datasource name.
     *
     * @param datasource The datasource name used by the Oracle cache
     */
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * Gets the SQL object prefix.
     *
     * @return The SQL object prefix used by the Oracle cache
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the SQL object prefix.
     *
     * @param prefix The SQL object prefix used by the Oracle cache
     */
    public void setPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            LOG.warn("Oracle cache prefix is blank; using default prefix '{}'", DEFAULT_PREFIX);
            this.prefix = DEFAULT_PREFIX;
        } else {
            this.prefix = prefix;
        }
    }
}
