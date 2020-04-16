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

import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

/**
 * @author Nirav Assar
 * @since 1.0
 */
@Testcontainers
class HazelcastClientSyncCacheSpec extends AbstractSyncCacheSpec implements HazelcastClientSupport {

    @Shared
    GenericContainer hazelcast = new GenericContainer("hazelcast/hazelcast:4.0")
            .withExposedPorts(5701)

    @Shared
    HazelcastInstance hazelcastServerInstance


    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "hazelcast.client.clusterName": 'dev',
                "hazelcast.client.network.addresses": ["127.0.0.1:${hazelcast.firstMappedPort}"]
        )
    }

}
