package io.micronaut.cache.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import org.testcontainers.containers.GenericContainer

trait HazelcastClientSupport {

    abstract GenericContainer getHazelcast()

    abstract HazelcastInstance getHazelcastServerInstance()
    abstract void setHazelcastServerInstance(HazelcastInstance hazelcastInstance)

    def setupSpec() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:${getHazelcast().firstMappedPort}");
        setHazelcastServerInstance(HazelcastClient.newHazelcastClient(clientConfig))
    }

    def cleanupSpec() {
        getHazelcastServerInstance().shutdown()
    }

}