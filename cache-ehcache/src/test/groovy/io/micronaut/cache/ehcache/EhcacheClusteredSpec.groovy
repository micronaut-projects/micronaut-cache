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
package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import org.ehcache.CacheManager
import org.ehcache.clustered.client.config.ClusteringServiceConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

import static org.ehcache.clustered.client.config.ClusteredResourceType.Types.*

@Testcontainers
class EhcacheClusteredSpec extends Specification {

    public GenericContainer terracotta = new GenericContainer("terracotta/terracotta-server-oss:5.6.4")
            .withExposedPorts(9410)

    void "it can create a clustered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.cluster.uri": "terracotta://localhost:${terracotta.firstMappedPort}/my-application",
                "ehcache.cluster.default-server-resource": "offheap-1",
                "ehcache.cluster.resource-pools.resource-pool-a.max-size": "8Mb",
                "ehcache.cluster.resource-pools.resource-pool-a.server-resource": "offheap-2",
                "ehcache.cluster.resource-pools.resource-pool-b.max-size": "10Mb",
                "ehcache.caches.clustered-cache.clustered-dedicated.server-resource": "offheap-1",
                "ehcache.caches.clustered-cache.clustered-dedicated.max-size": "8Mb",
                "ehcache.caches.shared-cache-1.clustered-shared.server-resource": "resource-pool-a",
                "ehcache.caches.shared-cache-2.clustered-shared.server-resource": "resource-pool-b"
        ])
        CacheManager ehcacheManager = ctx.getBean(CacheManager)
        io.micronaut.cache.CacheManager cacheManager = ctx.getBean(io.micronaut.cache.CacheManager)
        SyncCache clusteredCache = cacheManager.getCache('clustered-cache')
        SyncCache sharedCache1 = cacheManager.getCache('shared-cache-1')
        SyncCache sharedCache2 = cacheManager.getCache('shared-cache-2')

        expect:
        ehcacheManager.runtimeConfiguration.services[0] instanceof ClusteringServiceConfiguration
        ehcacheManager.runtimeConfiguration.services[0].connectionSource.clusterUri == URI.create("terracotta://localhost:${terracotta.firstMappedPort}/my-application")
        ehcacheManager.runtimeConfiguration.services[0].serverConfiguration.defaultServerResource == "offheap-1"
        ehcacheManager.runtimeConfiguration.services[0].serverConfiguration.resourcePools.size() == 2
        ehcacheManager.runtimeConfiguration.services[0].serverConfiguration.resourcePools['resource-pool-a'].size == 8 * 1024 * 1024
        ehcacheManager.runtimeConfiguration.services[0].serverConfiguration.resourcePools['resource-pool-a'].serverResource == "offheap-2"
        ehcacheManager.runtimeConfiguration.services[0].serverConfiguration.resourcePools['resource-pool-b'].size == 10 * 1024 * 1024

        clusteredCache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        clusteredCache.nativeCache.runtimeConfiguration.resourcePools.pools[DEDICATED].fromResource == "offheap-1"
        clusteredCache.nativeCache.runtimeConfiguration.resourcePools.pools[DEDICATED].size == 8 * 1024 * 1024

        sharedCache1.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        sharedCache1.nativeCache.runtimeConfiguration.resourcePools.pools[SHARED].sharedResourcePool == "resource-pool-a"

        sharedCache2.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        sharedCache2.nativeCache.runtimeConfiguration.resourcePools.pools[SHARED].sharedResourcePool == "resource-pool-b"
    }
}
