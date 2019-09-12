package io.micronaut.cache.hazelcast

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class HazelcastClientConfigurationSpec extends Specification {

    void "test nested network configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.instanceName": 'myInstance',
                "hazelcast.network-config.connectionTimeout": 99,
                "hazelcast.network-config.addresses": ['127.0.0.1:5701', 'http://hazelcast:5702'],
                "hazelcast.network-config.redoOperation": true
        ])

        when:
        HazelcastClientConfiguration hazelcastConfiguration = ctx.getBean(HazelcastClientConfiguration)

        then:
        hazelcastConfiguration.instanceName == "myInstance"
        hazelcastConfiguration.networkConfig.connectionTimeout == 99
        hazelcastConfiguration.networkConfig.addresses[0] == "127.0.0.1:5701"
        hazelcastConfiguration.networkConfig.addresses[1] == "http://hazelcast:5702"
        hazelcastConfiguration.networkConfig.redoOperation
    }
}
