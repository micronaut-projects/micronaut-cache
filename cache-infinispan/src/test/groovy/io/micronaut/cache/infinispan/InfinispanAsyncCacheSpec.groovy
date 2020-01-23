package io.micronaut.cache.infinispan

import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers

@Testcontainers
class InfinispanAsyncCacheSpec extends AbstractAsyncCacheSpec {

    public GenericContainer infinispan = new GenericContainer("infinispan/server:10.0.1.Final")
            .withExposedPorts(11222)
            .withEnv('USER', 'user')
            .withEnv('PASS', 'pass')


    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run([
                "infinispan.client.hotrod.force-return-values": true,
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": infinispan.firstMappedPort,
                "infinispan.client.hotrod.security.authentication.username": 'user',
                "infinispan.client.hotrod.security.authentication.password": 'pass',
                "infinispan.client.hotrod.security.authentication.realm": 'default',
                "infinispan.client.hotrod.security.authentication.server-name": 'infinispan'
        ])
    }

    @Override
    void flushCache(AsyncCache syncCache) {
    }

}
