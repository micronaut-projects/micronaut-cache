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
import io.micronaut.core.io.socket.SocketUtils
import spock.lang.Specification

class HazelcastMemberFactorySpec extends Specification {

    void "test hazelcast member instance is created"() {
        given:
        int port = SocketUtils.findAvailableTcpPort()
        System.setProperty("hazelcast.local.publicAddress", "localhost:$port")
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.instanceName": "myMemberInstance"
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myMemberInstance"

        cleanup:
        System.clearProperty("hazelcast.local.publicAddress")
        ctx.close()
    }

    void "test hazelcast member instance is created with custom config file"() {
        given:
        int port = SocketUtils.findAvailableTcpPort()
        File file = File.createTempFile("hazelcast-test-member-config-", ".yaml")
        file.text = """
hazelcast:
  instance-name: myCustomMemberInstance
  network:
    port:
      port: $port
"""
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.config": file.absolutePath
        ])

        when:
        HazelcastInstance hazelcastClientInstance = ctx.getBean(HazelcastInstance)

        then:
        hazelcastClientInstance.getName() == "myCustomMemberInstance"

        cleanup:
        System.clearProperty("hazelcast.config")
        ctx.close()
    }
}
