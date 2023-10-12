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
package io.micronaut.cache.hazelcast;

import com.hazelcast.config.Config;
import io.micronaut.cache.hazelcast.condition.HazelcastConfigResourceCondition;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;

/**
 * Configuration class for an Hazelcast as a member.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(value = "hazelcast", includes = {"properties", "licenseKey", "instanceName", "clusterName", "config"})
@Requires(condition = HazelcastConfigResourceCondition.class)
@Requires(missingBeans = Config.class)
@Requires(missingProperty = "hazelcast.client")
public class HazelcastMemberConfiguration extends Config {

    @Nullable
    private String config;

    /**
     * Default constructor.
     */
    public HazelcastMemberConfiguration() {
        super();
    }

    /**
     * Returns the path to a Hazelcast XML or YAML configuration file.
     * <p>If non-null, the contents of the file will override this configuration.
     * This path will be used to set system property {@code hazelcast.config}.</p>
     *
     * @since 4.1
     * @return The path to the Hazelcast XML or YAML configuration file.
     */
    @Nullable
    public String getConfig() {
        return config;
    }

    /**
     * Sets the path to a Hazelcast XML or YAML configuration file.
     * <p>If non-null, the contents of the file will override this configuration.
     * This path will be used to set system property {@code hazelcast.config}.</p>
     *
     * @param config The path to the Hazelcast XML or YAML configuration file.
     * @since 4.1
     */
    public void setConfig(@Nullable String config) {
        this.config = config;
    }
}
