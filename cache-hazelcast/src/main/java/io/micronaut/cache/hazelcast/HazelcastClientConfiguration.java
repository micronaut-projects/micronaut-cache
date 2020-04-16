/*
 * Copyright 2017-2020 original authors
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
import com.hazelcast.client.config.ConnectionRetryConfig;
import com.hazelcast.client.config.SocketOptions;
import io.micronaut.cache.hazelcast.condition.HazelcastConfigResourceCondition;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;

/**
 * Configuration class for an Hazelcast as a client.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@ConfigurationProperties(value = "hazelcast.client", includes = {"properties", "instanceName", "labels", "userContext",
                                                                 "clusterName"})
@Requires(condition = HazelcastConfigResourceCondition.class)
@Requires(missingBeans = ClientConfig.class)
@Requires(property = "hazelcast.client")
public class HazelcastClientConfiguration extends ClientConfig {

    @ConfigurationBuilder(value = "network", includes = {"smartRouting", "connectionTimeout", "addresses",
                                                         "redoOperation", "outboundPortDefinitions", "outboundPorts"})
    ClientNetworkConfig networkConfig = new ClientNetworkConfig();

    @ConfigurationBuilder(value = "connectionRetry", includes = {"initialBackoffMillis", "maxBackoffMillis"})
    ConnectionRetryConfig connectionRetryConfig = new ConnectionRetryConfig();

    @ConfigurationBuilder("network.socket")
    SocketOptions socketOptions = new SocketOptions();

    /**
     * Default constructor.
     */
    HazelcastClientConfiguration() {
        networkConfig.setSocketOptions(socketOptions);
        super.setNetworkConfig(networkConfig);
        super.getConnectionStrategyConfig().setConnectionRetryConfig(connectionRetryConfig);
    }
}
