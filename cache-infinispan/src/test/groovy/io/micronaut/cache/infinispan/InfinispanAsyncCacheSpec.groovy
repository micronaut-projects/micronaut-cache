package io.micronaut.cache.infinispan

import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

@Testcontainers
class InfinispanAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    GenericContainer infinispan = new GenericContainer("infinispan/server")
            .withExposedPorts(11222)
            .withEnv('USER', 'user')
            .withEnv('PASS', 'pass')


    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run([
                "infinispan.client.hotrod.force-return-values": true,
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": infinispan.firstMappedPort
        ])
    }

}
