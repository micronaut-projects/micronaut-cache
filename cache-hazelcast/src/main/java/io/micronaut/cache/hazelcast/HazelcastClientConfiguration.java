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

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.SocketOptions;
import com.hazelcast.config.GroupConfig;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration class for an Hazelcast as a client.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@ConfigurationProperties(value = "hazelcast.client", includes = {"properties", "executorPoolSize", "licenseKey", "instanceName",
    "labels", "userContext"})
public class HazelcastClientConfiguration extends ClientConfig {

    @ConfigurationBuilder(value = "network", includes = {"smartRouting", "connectionAttemptPeriod", "connectionAttemptLimit",
        "connectionTimeout", "addresses", "redoOperation", "outboundPortDefinitions", "outboundPorts"})
    ClientNetworkConfig networkConfig = new ClientNetworkConfig();

    @ConfigurationBuilder("network.socket")
    SocketOptions socketOptions = new SocketOptions();

    @ConfigurationBuilder("group")
    GroupConfig groupConfig = new GroupConfig();

    /**
     * Default constructor.
     */
    HazelcastClientConfiguration() {
        networkConfig.setSocketOptions(socketOptions);
        super.setNetworkConfig(networkConfig);
        super.setGroupConfig(groupConfig);
    }
}
