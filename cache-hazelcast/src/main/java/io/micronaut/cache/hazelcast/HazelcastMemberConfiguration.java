/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import io.micronaut.cache.hazelcast.condition.HazelcastConfigResourceCondition;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;

/**
 * Configuration class for an Hazelcast as a member.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(value = "hazelcast", includes = {"properties", "licenseKey", "instanceName"})
@Requires(condition = HazelcastConfigResourceCondition.class)
@Requires(missingBeans = Config.class)
@Requires(missingProperty = "hazelcast.client")
public class HazelcastMemberConfiguration extends Config {

    @ConfigurationBuilder("group")
    GroupConfig groupConfig = new GroupConfig();

    /**
     * Default constructor.
     */
    public HazelcastMemberConfiguration() {
        super.setGroupConfig(groupConfig);
    }
}
