package io.micronaut.cache.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizeConfig
import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

@Testcontainers
class HazelcastClientAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    public GenericContainer hazelcast = new GenericContainer("hazelcast/hazelcast")
            .withExposedPorts(5701)

    @Shared
    HazelcastInstance hazelcastServerInstance

    def setupSpec() {
        MapConfig mapConfig = new MapConfig()
                .setMaxSizeConfig(new MaxSizeConfig()
                        .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_PARTITION)
                        .setSize(3))
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setName("test")
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:${hazelcast.firstMappedPort}");
        hazelcastServerInstance = HazelcastClient.newHazelcastClient(clientConfig)
        hazelcastServerInstance.config.addMapConfig(mapConfig)
    }

    def cleanupSpec() {
        hazelcastServerInstance.shutdown()
    }

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "hazelcast.client.network.addresses": ["127.0.0.1:${hazelcast.firstMappedPort}"]
        )
    }

    @Override
    void flushCache(AsyncCache syncCache) {

    }
}
