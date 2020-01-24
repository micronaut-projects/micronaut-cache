package io.micronaut.cache.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizeConfig
import com.hazelcast.core.HazelcastInstance
import org.testcontainers.containers.GenericContainer


trait HazelcastClientSupport {

    abstract GenericContainer getHazelcast()

    abstract HazelcastInstance getHazelcastServerInstance()
    abstract void setHazelcastServerInstance(HazelcastInstance hazelcastInstance)

    def setupSpec() {
        MapConfig mapConfig = new MapConfig()
                .setMaxSizeConfig(new MaxSizeConfig()
                        .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_PARTITION)
                        .setSize(3))
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setName("test")
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:${getHazelcast().firstMappedPort}");
        setHazelcastServerInstance(HazelcastClient.newHazelcastClient(clientConfig))
        getHazelcastServerInstance().config.addMapConfig(mapConfig)
    }

    def cleanupSpec() {
        getHazelcastServerInstance().shutdown()
    }

}