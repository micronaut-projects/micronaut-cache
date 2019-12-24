package io.micronaut.cache.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizeConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import spock.lang.Shared

class HazelcastAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    HazelcastInstance hazelcastServerInstance

    def setupSpec() {
        MapConfig mapConfig = new MapConfig()
                .setMaxSizeConfig(new MaxSizeConfig()
                        .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_PARTITION)
                        .setSize(3))
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setName("test")
        Config config = new Config("sampleCache")
        config.setProperty("hazelcast.partition.count", "1")
        config.addMapConfig(mapConfig)
        hazelcastServerInstance = Hazelcast.getOrCreateHazelcastInstance(config)
    }

    def cleanupSpec() {
        hazelcastServerInstance.shutdown()
    }

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "hazelcast.client.instanceName": "sampleCache",
                "hazelcast.client.network.addresses": ['127.0.0.1:5701']
        )
    }

    @Override
    void flushCache(AsyncCache syncCache) {

    }
}
