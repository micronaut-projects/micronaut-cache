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
package io.micronaut.cache.ignite.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.apache.ignite.configuration.ClientConfiguration;

/**
 * Ignite configuration.
 */
@EachProperty(value = "ignite.clients", primary = "default")
public class IgniteClientConfiguration implements Named{
    private final String name;

    @ConfigurationBuilder
    private final ClientConfiguration client = new ClientConfiguration();


    /**
     * @param name Name or key of the client.
     */
    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    /**
     * @return client configuration
     */
    public ClientConfiguration getClient() {
        return client;
    }

    @NonNull
    @Override
    public String getName() {
        return this.name;
    }

}
