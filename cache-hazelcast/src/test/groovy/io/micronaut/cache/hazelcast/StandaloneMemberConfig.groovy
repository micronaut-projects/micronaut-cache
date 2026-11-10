package io.micronaut.cache.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.config.JoinConfig
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton

/**
 * Disables every cluster join mechanism for embedded Hazelcast members created in tests.
 * Without this, members use multicast auto-discovery with the default cluster name and can
 * discover members from other test JVMs, stale processes or other machines on the network,
 * which makes start-up and shutdown wait on other nodes (up to the 5 minutes join timeout).
 */
@Singleton
class StandaloneMemberConfig implements BeanCreatedEventListener<Config> {

    @Override
    Config onCreated(BeanCreatedEvent<Config> event) {
        Config config = event.bean
        JoinConfig join = config.networkConfig.join
        join.autoDetectionConfig.enabled = false
        join.multicastConfig.enabled = false
        join.tcpIpConfig.enabled = false
        config
    }
}
