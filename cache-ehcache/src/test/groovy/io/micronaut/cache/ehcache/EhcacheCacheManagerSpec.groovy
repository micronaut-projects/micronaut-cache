package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import spock.lang.Specification

import static org.ehcache.config.ResourceType.Core.HEAP

class EhcacheCacheManagerSpec extends Specification {

    void "it create caches from configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.keyType": "java.lang.Long",
                "ehcache.caches.foo.valueType": "java.lang.String"
        ])

        when:
        EhcacheCacheManager ehcacheCacheManager = ctx.getBean(EhcacheCacheManager)

        then:
        ehcacheCacheManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = ehcacheCacheManager.getCache('foo')
        cache.nativeCache.runtimeConfiguration.keyType == Long
        cache.nativeCache.runtimeConfiguration.valueType == String
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 10
    }

    void "it can create entries-based heap tiered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27
        ])
        EhcacheCacheManager ehcacheManager = ctx.getBean(EhcacheCacheManager)
        SyncCache cache = ehcacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == EntryUnit.ENTRIES
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 27
    }

    void "it can create size-based heap tiered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-size": '15Mb'
        ])

        EhcacheCacheManager ehcacheManager = ctx.getBean(EhcacheCacheManager)
        SyncCache cache = ehcacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 15 * 1024 * 1024
    }

}
