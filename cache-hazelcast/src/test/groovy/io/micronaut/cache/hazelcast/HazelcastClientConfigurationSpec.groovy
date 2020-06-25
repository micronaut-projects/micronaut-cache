package io.micronaut.cache.hazelcast

import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.ListenerConfig
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import spock.lang.Specification

import javax.inject.Singleton

@spock.lang.Retry
class HazelcastClientConfigurationSpec extends Specification {

    void "test nested network configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.client.network.smartRouting": false,
                "hazelcast.client.network.connectionTimeout": 1000,
                "hazelcast.client.network.addresses": ['127.0.0.1:5701', 'http://hazelcast:5702'],
                "hazelcast.client.network.redoOperation": true,
                "hazelcast.client.network.outboundPortDefinitions": ["a", "b"],
                "hazelcast.client.network.outboundPorts": [1, 2],
                "hazelcast.client.network.socket.tcpNoDelay": false,
                "hazelcast.client.network.socket.keepAlive": false,
                "hazelcast.client.network.socket.reuseAddress": false,
                "hazelcast.client.network.socket.lingerSeconds": 5,
                "hazelcast.client.network.socket.bufferSize": 64,
                "hazelcast.client.connectionRetry.initialBackoffMillis": 500,
                "hazelcast.client.connectionRetry.maxBackoffMillis": 5000,
                "hazelcast.client.clusterName": "ClusterName",
                "hazelcast.client.properties": [x: "x", y: "y"],
                "hazelcast.client.instanceName": "instance name",
                "hazelcast.client.labels": ["a", "b"],
                "hazelcast.client.userContext": [a: "a", b: "b"]
        ])

        when:
        HazelcastClientConfiguration hazelcastClientConfiguration = ctx.getBean(HazelcastClientConfiguration)

        then:
        !hazelcastClientConfiguration.networkConfig.smartRouting
        hazelcastClientConfiguration.networkConfig.addresses[0] == "127.0.0.1:5701"
        hazelcastClientConfiguration.networkConfig.addresses[1] == "http://hazelcast:5702"
        hazelcastClientConfiguration.networkConfig.redoOperation
        hazelcastClientConfiguration.networkConfig.outboundPortDefinitions == ["a", "b"]
        hazelcastClientConfiguration.networkConfig.outboundPorts == [1, 2]
        !hazelcastClientConfiguration.networkConfig.socketOptions.tcpNoDelay
        !hazelcastClientConfiguration.networkConfig.socketOptions.keepAlive
        !hazelcastClientConfiguration.networkConfig.socketOptions.reuseAddress
        hazelcastClientConfiguration.networkConfig.socketOptions.lingerSeconds == 5
        hazelcastClientConfiguration.networkConfig.socketOptions.bufferSize == 64
        hazelcastClientConfiguration.connectionRetryConfig.initialBackoffMillis == 500
        hazelcastClientConfiguration.connectionRetryConfig.maxBackoffMillis == 5000
        hazelcastClientConfiguration.clusterName == "ClusterName"
        hazelcastClientConfiguration.properties.get("x") == "x"
        hazelcastClientConfiguration.properties.get("y") == "y"
        hazelcastClientConfiguration.instanceName == "instance name"
        hazelcastClientConfiguration.labels == ["a", "b"] as Set
        hazelcastClientConfiguration.userContext.get("a") == "a"
        hazelcastClientConfiguration.userContext.get("b") == "b"
        hazelcastClientConfiguration.listenerConfigs.size() == 1
    }

    @Singleton
    static class CustomConfig implements BeanCreatedEventListener<ClientConfig> {

        @Override
        ClientConfig onCreated(BeanCreatedEvent<ClientConfig> event) {
            event.getBean().addListenerConfig(new ListenerConfig(new EventListener() {

            }))
            event.getBean()
        }
    }
}
