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
package io.micronaut.cache.oracle.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Global datasource selection for the Oracle cache module.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
@ConfigurationProperties("micronaut.cache.oracle")
public final class OracleCacheDataSourceConfiguration {

    private String datasource;
    private String prefix;

    public String getDatasource() {
        if (datasource == null || datasource.isBlank()) {
            throw new IllegalStateException("micronaut.cache.oracle.datasource must be configured");
        }
        return datasource;
    }

    public boolean isDatasourceConfigured() {
        return datasource != null && !datasource.isBlank();
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getPrefix() {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalStateException("micronaut.cache.oracle.prefix must be configured");
        }
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
