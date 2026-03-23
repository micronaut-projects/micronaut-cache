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
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Startup listener delegating Oracle cache schema bootstrap to a manual JDBC executor.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
@Singleton
public class OracleCacheSchemaInitializer implements ApplicationEventListener<ApplicationStartupEvent> {

    private static final String DEFAULT_RESOURCE_PATH = "db/oracle-cache.sql";

    private final OracleCacheSchemaExecutor schemaExecutor;

    public OracleCacheSchemaInitializer(BeanContext beanContext,
                                        ResourceResolver resourceResolver,
                                        OracleCacheDataSourceConfiguration dataSourceConfiguration,
                                        List<OracleCacheConfiguration> cacheConfigurations) {
        this.schemaExecutor = new OracleCacheSchemaExecutor(
            beanContext,
            resourceResolver,
            dataSourceConfiguration,
            DEFAULT_RESOURCE_PATH,
            cacheConfigurations
        );
    }

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        schemaExecutor.initializeSchemaNow();
    }

    void initializeSchemaManually() {
        schemaExecutor.initializeSchemaNow();
    }
}
