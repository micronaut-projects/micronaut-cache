/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizeConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.SyncCache
import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext
import spock.lang.Retry
import spock.lang.Shared

/**
 * @author Nirav Assar
 * @since 1.0
 */
@Retry
class HazelcastSyncCacheSpec extends AbstractSyncCacheSpec {

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
    void flushCache(SyncCache syncCache) {
    }

}
