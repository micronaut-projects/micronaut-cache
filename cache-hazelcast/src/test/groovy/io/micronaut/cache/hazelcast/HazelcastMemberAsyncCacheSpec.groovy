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
import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import spock.lang.Retry

import javax.inject.Singleton

/**
 * @since 1.0
 */
@Retry
class HazelcastMemberAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Singleton
    static class CustomConfig implements BeanCreatedEventListener<Config> {
        @Override
        Config onCreated(BeanCreatedEvent<Config> event) {
            MapConfig mapConfig = new MapConfig()
                    .setMaxSizeConfig(new MaxSizeConfig()
                            .setMaxSizePolicy(MaxSizeConfig.MaxSizePolicy.PER_PARTITION)
                            .setSize(3))
                    .setEvictionPolicy(EvictionPolicy.LRU)
                    .setName("test")
            event.getBean().addMapConfig(mapConfig)
            event.getBean().setInstanceName("sampleCache")
            event.getBean().setProperty("hazelcast.partition.count", "1")

            event.getBean()
        }
    }

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run()
    }

    @Override
    void flushCache(AsyncCache syncCache) {

    }
}
