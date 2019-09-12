package io.micronaut.cache.hazelcast;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.ClientSecurityConfig;
import com.hazelcast.config.GroupConfig;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("hazelcast")
public class HazelcastClientConfiguration extends ClientConfig {

    HazelcastClientConfiguration() {
        super.setNetworkConfig(this.networkConfig);
        super.setGroupConfig(this.groupConfig);
    }

    @ConfigurationBuilder("network-config")
    ClientNetworkConfig networkConfig = new ClientNetworkConfig();

    @ConfigurationBuilder("group-config")
    GroupConfig groupConfig = new GroupConfig();
}
