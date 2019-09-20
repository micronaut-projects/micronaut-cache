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
import com.hazelcast.core.HazelcastInstance;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

/**
 * Factory class that creates a {@link HazelcastInstance}.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@Factory
public class HazelcastClientFactory {

    /**
     * Create a singleton {@link HazelcastInstance} client, based on configuration.
     *
     * @param hazelcastClientConfiguration the configuration read it as a bean
     * @return {@link HazelcastInstance}
     */
    @Singleton
    public HazelcastInstance hazelcastClientInstance(HazelcastClientConfiguration hazelcastClientConfiguration) {
        return HazelcastClient.newHazelcastClient(hazelcastClientConfiguration);
    }
}
