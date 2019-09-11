package io.micronaut.cache.hazelcast;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("hazelcast")
// we could inject the configurations here and call add mapConfig here.
public class HazelcastConfiguration extends ClientConfig {

    HazelcastConfiguration(List<HazelcastCacheConfiguration> cacheConfigurations) {
        for (HazelcastCacheConfiguration hazelcastCacheConfiguration : cacheConfigurations) {
            this.addMapConfig(hazelcastCacheConfiguration);
        }
    }
}
