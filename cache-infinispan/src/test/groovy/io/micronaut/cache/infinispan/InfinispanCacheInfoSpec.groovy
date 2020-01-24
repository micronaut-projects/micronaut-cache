package io.micronaut.cache.infinispan

import io.micronaut.cache.CacheInfo
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.reactivex.Flowable
import org.infinispan.client.hotrod.RemoteCache
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class InfinispanCacheInfoSpec extends Specification {

    @Shared
    GenericContainer infinispan = new GenericContainer("infinispan/server")
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
                "infinispan.client.hotrod.security.authentication.username": 'user',
                "infinispan.client.hotrod.security.authentication.password": 'pass',
                "infinispan.client.hotrod.security.authentication.realm": 'default',
                "infinispan.client.hotrod.security.authentication.server-name": 'infinispan'
        ])
        InfinispanCacheManager cacheManager = applicationContext.getBean(InfinispanCacheManager)
        SyncCache<RemoteCache<Object, Object>> cache = cacheManager.getCache("InfinispanCacheInfoSpec")
        InfinispanHotRodClientConfiguration configuration = applicationContext.getBean(InfinispanHotRodClientConfiguration)

        expect:
        configuration.statistics.create().enabled()

        when:
        CacheInfo cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()

        then:
        cacheInfo.get()['implementationClass'] == 'org.infinispan.client.hotrod.impl.RemoteCacheImpl'
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 0
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 0

        when:
        cache.put("foo", "bar")

        then:
        cache.get("foo", Argument.of(String)).get() == "bar"

        when:
        cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()

        then:
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 1
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 2

        cleanup:
        applicationContext.close()
    }
}
