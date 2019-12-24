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

    public HazelcastMemberConfiguration() {
        super.setGroupConfig(groupConfig);
    }
}
