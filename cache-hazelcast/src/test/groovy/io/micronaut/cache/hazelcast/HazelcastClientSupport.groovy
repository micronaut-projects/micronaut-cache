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
        clientConfig.setClusterName("dev");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:${getHazelcast().firstMappedPort}");
        setHazelcastServerInstance(HazelcastClient.newHazelcastClient(clientConfig))
    }

    def cleanupSpec() {
        getHazelcastServerInstance().shutdown()
    }

}