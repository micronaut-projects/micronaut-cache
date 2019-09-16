package io.micronaut.cache.hazelcast

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification

/**
 * TODO: javadoc
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
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
                "hazelcast.instanceName": "myInstance",
                "hazelcast.network.addresses": ['127.0.0.1:5701'],
                "hazelcast.network.connectionTimeout": 99
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myInstance"
    }
}
