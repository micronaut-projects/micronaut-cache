package io.micronaut.cache.hazelcast

import com.hazelcast.core.IMap
import com.hazelcast.map.impl.proxy.MapProxyImpl
import io.micronaut.cache.SyncCache
import io.micronaut.cache.exceptions.CacheSystemException
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

/**
 * TODO: javadoc
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
class HazelcastManagerSpec extends Specification {

    void "test configuration is loaded to create cache"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.timeToLiveSeconds": 25
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)

        then:
        hazelcastManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = hazelcastManager.getCache('foo')
        cache.name == 'foo'
        cache.configuration.getTimeToLiveSeconds() == 25
    }

    void "test multiple caches are created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.timeToLiveSeconds": 25,
                "hazelcast.caches.bar.timeToLiveSeconds": 99
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)

        then:
        hazelcastManager.cacheNames.size() == 2

        and:
        hazelcastManager.getCache('foo')
        hazelcastManager.getCache('bar')
    }

    void "test property configurations are set"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.backupCount": 3,
                "hazelcast.caches.foo.timeToLiveSeconds": 299
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)
        SyncCache<IMap> cache = hazelcastManager.getCache('foo')
        IMap nativeCache = cache.nativeCache

        then:
        ((MapProxyImpl) nativeCache).getTotalBackupCount() == 3
    }

    void "test default hazelcast props set to true"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.useDefaultHazelcastXml": true
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)
        SyncCache<IMap> cache = hazelcastManager.getCache('foo')
        IMap nativeCache = cache.nativeCache

        then:
        ((MapProxyImpl) nativeCache).getTotalBackupCount() == 1
    }

    void "test maximumSize must be with maximumSizePolicy"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.maximumSize": 10
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)
        SyncCache<IMap> cache = hazelcastManager.getCache('foo')

        then:
        thrown CacheSystemException
    }

    void "test maximumSize and maximumSizePolicy defined"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.maximumSize": 15,
                "hazelcast.caches.foo.maximumSizePolicy": "PER_NODE"
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)
        SyncCache<IMap> cache = hazelcastManager.getCache('foo')

        then:
        notThrown CacheSystemException
    }
}
