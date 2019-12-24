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
package io.micronaut.cache.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.micronaut.cache.hazelcast.condition.HazelcastConfigResourceCondition;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

/**
 * Factory class that creates a {@link HazelcastInstance}.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@Factory
public class HazelcastFactory {

    /**
     * Create a singleton {@link HazelcastInstance} client, based on configuration.
     *
     * @param clientConfig the configuration read it as a bean
     * @return {@link HazelcastInstance}
     */
    @Requires(beans = ClientConfig.class)
    @Singleton
    public HazelcastInstance hazelcastInstance(ClientConfig clientConfig) {
        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Create a singleton {@link HazelcastInstance} member, based on configuration.
     *
     * @param config the configuration read it as a bean
     * @return {@link HazelcastInstance}
     */
    @Requires(beans = Config.class)
    @Singleton
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Create a singleton {@link HazelcastInstance} member or client, if a default Hazelcast configuration resource exists.
     *
     * @return {@link HazelcastInstance}
     */
    @Requires(missingBeans = {Config.class, ClientConfig.class})
    @Singleton
    public HazelcastInstance hazelcastInstance() {
        if (HazelcastConfigResourceCondition.resourceExists("hazelcast-client.xml", "hazelcast-client.yml")){
            return HazelcastClient.newHazelcastClient();
        }
        return Hazelcast.newHazelcastInstance();
    }
}
