/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Named;
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
     * Create a singleton {@link HazelcastInstance} client, based on an existing {@link ClientConfig} bean.
     *
     * @param clientConfig the configuration read it as a bean
     * @return {@link HazelcastInstance}
     */
    @Requires(beans = ClientConfig.class)
    @Singleton
    @Bean(preDestroy = "shutdown")
    public HazelcastInstance hazelcastInstance(ClientConfig clientConfig) {
        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Create a singleton {@link HazelcastInstance} member, based on an existing {@link Config} bean.
     *
     * @param config the configuration read it as a bean
     * @return {@link HazelcastInstance}
     */
    @Requires(beans = Config.class)
    @Singleton
    @Bean(preDestroy = "shutdown")
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Create a singleton {@link HazelcastInstance} client, if client config resource exists.
     *
     * @return {@link HazelcastInstance}
     */
    @Requires(missingBeans = {Config.class, ClientConfig.class})
    @Requires(condition = HazelcastConfigResourceCondition.HazelcastClientConfigCondition.class)
    @Singleton
    @Named("hazelcastInstance")
    @Bean(preDestroy = "shutdown")
    public HazelcastInstance hazelcastClient() {
        return HazelcastClient.newHazelcastClient();
    }

    /**
     * Create a singleton {@link HazelcastInstance} instance, if instance config resource exists.
     *
     * @return {@link HazelcastInstance}
     */
    @Requires(missingBeans = {Config.class, ClientConfig.class})
    @Requires(condition = HazelcastConfigResourceCondition.HazelcastInstanceConfigCondition.class)
    @Singleton
    @Bean(preDestroy = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        return Hazelcast.newHazelcastInstance();
    }
}
