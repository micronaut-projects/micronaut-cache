package io.micronaut.cache.hazelcast

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class HazelcastClientConfigurationSpec extends Specification {

    void "test nested network configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.instanceName": 'myInstance',
                "hazelcast.network.connectionTimeout": 99,
                "hazelcast.network.addresses": ['127.0.0.1:5701', 'http://hazelcast:5702'],
                "hazelcast.network.redoOperation": true
        ])

        when:
        HazelcastClientConfiguration hazelcastClientConfiguration = ctx.getBean(HazelcastClientConfiguration)

        then:
        hazelcastClientConfiguration.instanceName == "myInstance"
        hazelcastClientConfiguration.networkConfig.connectionTimeout == 99
        hazelcastClientConfiguration.networkConfig.addresses[0] == "127.0.0.1:5701"
        hazelcastClientConfiguration.networkConfig.addresses[1] == "http://hazelcast:5702"
        hazelcastClientConfiguration.networkConfig.redoOperation
    }
}
