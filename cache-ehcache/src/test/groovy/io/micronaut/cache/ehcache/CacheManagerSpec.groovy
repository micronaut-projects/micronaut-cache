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

import io.micronaut.cache.CacheInfo
import io.micronaut.cache.CacheManager
import io.reactivex.Flowable
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.core.spi.service.StatisticsService
import spock.lang.Specification

import static org.ehcache.config.ResourceType.Core.*

class CacheManagerSpec extends Specification {

    void "it create caches from configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.keyType": "java.lang.Long",
                "ehcache.caches.foo.valueType": "java.lang.String"
        ])

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        cacheManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = cacheManager.getCache('foo')
        cache.nativeCache.runtimeConfiguration.keyType == Long
        cache.nativeCache.runtimeConfiguration.valueType == String
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 100

        cleanup:
        ctx.close()
    }

    void "it can create entries-based heap tiered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27
        ])
        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == EntryUnit.ENTRIES
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 27

        cleanup:
        ctx.close()
    }

    void "it can create size-based heap tiered cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-size": '15Mb'
        ])

        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 15 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it can create an offheap tier"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.offheap.max-size": '23Mb'
        ])

        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].size == 23 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it can create a disk tier"() {
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.disk.max-size": '50Mb',
                "ehcache.storage-path": "/tmp/${System.currentTimeMillis()}"
        ])

        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 1
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].size == 50 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it can configure disk segments"() {
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.disk.max-size": '50Mb',
                "ehcache.caches.foo.disk.segments": 2,
                "ehcache.storage-path": "/tmp/${System.currentTimeMillis()}"
        ])

        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.config.serviceConfigurations[0].diskSegments == 2

        cleanup:
        ctx.close()
    }

    void "it can create heap + offheap tiers"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27,
                "ehcache.caches.foo.offheap.max-size": '23Mb'
        ])
        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 2
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == EntryUnit.ENTRIES
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 27
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].size == 23 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it can create heap + offheap + disk tiers"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27,
                "ehcache.caches.foo.offheap.max-size": '23Mb',
                "ehcache.caches.foo.disk.max-size": '50Mb',
                "ehcache.storage-path": "/tmp/${System.currentTimeMillis()}"
        ])
        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 3
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == EntryUnit.ENTRIES
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 27
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[OFFHEAP].size == 23 * 1024 * 1024
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].size == 50 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it can create heap + disk tiers"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27,
                "ehcache.caches.foo.disk.max-size": '50Mb',
                "ehcache.storage-path": "/tmp/${System.currentTimeMillis()}"
        ])
        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')

        expect:
        cache.nativeCache.runtimeConfiguration.resourcePools.pools.size() == 2
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].unit == EntryUnit.ENTRIES
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[HEAP].size == 27
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].unit == MemoryUnit.B
        cache.nativeCache.runtimeConfiguration.resourcePools.pools[DISK].size == 50 * 1024 * 1024

        cleanup:
        ctx.close()
    }

    void "it publishes cache info"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "ehcache.caches.foo.heap.max-entries": 27
        ])
        CacheManager cacheManager = ctx.getBean(CacheManager)
        SyncCache cache = cacheManager.getCache('foo')
        CacheInfo cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()

        expect:
        cacheInfo.get()['implementationClass'] == 'org.ehcache.core.Ehcache'
        cacheInfo.get()['ehcache']['cachePuts'] == 0


        when:
        cache.put("foo", "bar")

        then:
        cacheInfo.get()['ehcache']['cachePuts'] == 1

        cleanup:
        ctx.close()
    }
}
