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
package io.micronaut.cache.infinispan

import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.IgnoreIf
import spock.lang.Retry
import spock.lang.Shared

@Testcontainers
@Retry
@IgnoreIf({System.getenv('GITHUB_WORKFLOW')})
class InfinispanAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    GenericContainer infinispan = new GenericContainer("infinispan/server:${System.getProperty('infinispanVersion')}")
            .withExposedPorts(11222)
            .withEnv('USER', 'user')
            .withEnv('PASS', 'pass')


    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run([
                "infinispan.client.hotrod.force-return-values": true,
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": infinispan.firstMappedPort,
                "infinispan.client.hotrod.security.authentication.username": "user",
                "infinispan.client.hotrod.security.authentication.password": "pass",
                "infinispan.client.hotrod.security.authentication.realm": "default",
                "infinispan.client.hotrod.security.authentication.server-name": "infinispan",
                "infinispan.client.hotrod.security.authentication.sasl-mechanism": "DIGEST-MD5"
        ])
    }

}
