/*
 * Copyright 2017-2020 original authors
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
import com.hazelcast.config.MaxSizePolicy
import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton

/**
 * @since 1.0
 */
class HazelcastMemberSyncCacheSpec extends AbstractSyncCacheSpec {

    @Singleton
    static class CustomConfig implements BeanCreatedEventListener<Config> {
        @Override
        Config onCreated(BeanCreatedEvent<Config> event) {
            MapConfig mapConfig = new MapConfig().setName("test")
            mapConfig.getEvictionConfig()
                    .setMaxSizePolicy(MaxSizePolicy.PER_PARTITION)
                    .setSize(3)
                    .setEvictionPolicy(EvictionPolicy.LRU)
            event.getBean().addMapConfig(mapConfig)
            event.getBean()
        }
    }

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run()
    }

}
