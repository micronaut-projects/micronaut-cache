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
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class HazelcastClientFactorySpec extends Specification implements HazelcastClientSupport {

    @Shared
    GenericContainer hazelcast = new GenericContainer("hazelcast/hazelcast:" + System.getProperty('hazelcastVersion'))
            .withExposedPorts(5701)

    @Shared
    HazelcastInstance hazelcastServerInstance

    void "test hazelcast client instance is created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.client.clusterName": 'dev',
                "hazelcast.client.instanceName": "myInstance",
                "hazelcast.client.network.addresses": ["127.0.0.1:${hazelcast.firstMappedPort}"],
                "hazelcast.client.network.connectionTimeout": 99
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myInstance"

        cleanup:
        ctx.close()
    }

    void "test hazelcast client instance is created with custom config file"() {
        given:
        File file = File.createTempFile("hazelcast-test-client-config-", ".yaml")
        file.text = """
hazelcast-client:
  cluster-name: dev
  instance-name: myCustomClientInstance
  network:
    cluster-members:
    - 127.0.0.1:${hazelcast.firstMappedPort}
    connection-timeout: 99
"""
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.client.config": file.absolutePath
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myCustomClientInstance"

        cleanup:
        ctx.close()
        System.clearProperty("hazelcast.client.config")
    }
}
