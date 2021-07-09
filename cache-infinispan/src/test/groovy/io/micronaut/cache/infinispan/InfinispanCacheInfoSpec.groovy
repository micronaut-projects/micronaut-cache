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

import io.micronaut.cache.CacheInfo
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import org.infinispan.client.hotrod.RemoteCache
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import reactor.core.publisher.Flux
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class InfinispanCacheInfoSpec extends Specification {

    @Shared
    GenericContainer infinispan = new GenericContainer("infinispan/server:${System.getProperty('infinispanVersion')}")
            .withExposedPorts(11222)
            .withEnv('USER', 'user')
            .withEnv('PASS', 'pass')

    void "it publishes cache info stats"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                "infinispan.client.hotrod.statistics.enabled": true,
                "infinispan.client.hotrod.force-return-values": true,
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": infinispan.firstMappedPort,
                "infinispan.client.hotrod.security.authentication.username": "user",
                "infinispan.client.hotrod.security.authentication.password": "pass",
                "infinispan.client.hotrod.security.authentication.realm": "default",
                "infinispan.client.hotrod.security.authentication.server-name": "infinispan",
                "infinispan.client.hotrod.security.authentication.sasl-mechanism": "DIGEST-MD5"
        ])
        InfinispanCacheManager cacheManager = applicationContext.getBean(InfinispanCacheManager)
        SyncCache<RemoteCache<Object, Object>> cache = cacheManager.getCache("InfinispanCacheInfoSpec")
        InfinispanHotRodClientConfiguration configuration = applicationContext.getBean(InfinispanHotRodClientConfiguration)

        expect:
        configuration.statistics.create().enabled()

        when:
        CacheInfo cacheInfo = Flux.from(cache.cacheInfo).blockFirst()

        then:
        cacheInfo.get()['implementationClass'] == 'org.infinispan.client.hotrod.impl.RemoteCacheImpl'
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 0
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 0

        when:
        cache.put("foo", "bar")

        then:
        cache.get("foo", Argument.of(String)).get() == "bar"

        when:
        cacheInfo = Flux.from(cache.cacheInfo).blockFirst()

        then:
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 1
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 2

        cleanup:
        applicationContext.close()
    }
}
