package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import org.ehcache.clustered.client.config.ClusteringServiceConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@Testcontainers
class EhcacheClusteredSpec extends Specification {

    public GenericContainer terracotta = new GenericContainer("terracotta/terracotta-server-oss:5.6.4")
            .withExposedPorts(9410)

    void "it can create a clustered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.cluster.uri": "terracotta://localhost:${terracotta.firstMappedPort}/my-application",
                "ehcache.cluster.default-server-resource": "offheap-1",
                "ehcache.caches.foo.enabled": true
        ])
        EhcacheCacheManager ehcacheManager = ctx.getBean(EhcacheCacheManager)
        SyncCache cache = ehcacheManager.getCache('foo')

        expect:
        ehcacheManager.cacheManager.runtimeConfiguration.services[0] instanceof ClusteringServiceConfiguration
        ehcacheManager.cacheManager.runtimeConfiguration.services[0].connectionSource.clusterUri == URI.create("terracotta://localhost:${terracotta.firstMappedPort}/my-application")
        ehcacheManager.cacheManager.runtimeConfiguration.services[0].serverConfiguration.defaultServerResource == "offheap-1"
    }
}
