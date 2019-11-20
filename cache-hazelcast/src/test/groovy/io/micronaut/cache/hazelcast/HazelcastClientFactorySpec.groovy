package io.micronaut.cache.hazelcast

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification

class HazelcastClientFactorySpec extends Specification {

    @Shared
    HazelcastInstance hazelcastServerInstance

    def setupSpec() {
        hazelcastServerInstance = Hazelcast.newHazelcastInstance()
    }

    def cleanupSpec() {
        hazelcastServerInstance.shutdown()
    }

    void "test hazelcast client instance is created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.client.instanceName": "myInstance",
                "hazelcast.client.network.addresses": ['127.0.0.1:5701'],
                "hazelcast.client.network.connectionTimeout": 99
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myInstance"
    }
}
