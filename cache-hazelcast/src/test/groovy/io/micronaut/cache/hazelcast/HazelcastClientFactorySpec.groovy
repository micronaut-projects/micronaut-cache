package io.micronaut.cache.hazelcast

import com.hazelcast.core.HazelcastInstance
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class HazelcastClientFactorySpec extends Specification implements HazelcastClientSupport {

    @Shared
    GenericContainer hazelcast = new GenericContainer("hazelcast/hazelcast:3.12.6")
            .withExposedPorts(5701)

    @Shared
    HazelcastInstance hazelcastServerInstance

    void "test hazelcast client instance is created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.client.group.name": 'dev',
                "hazelcast.client.instanceName": "myInstance",
                "hazelcast.client.network.addresses": ["127.0.0.1:${hazelcast.firstMappedPort}"],
                "hazelcast.client.network.connectionTimeout": 99
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myInstance"
    }
}
