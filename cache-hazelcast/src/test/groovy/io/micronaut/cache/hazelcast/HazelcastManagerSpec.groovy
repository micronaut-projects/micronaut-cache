package io.micronaut.cache.hazelcast

import com.hazelcast.core.IMap
import com.hazelcast.map.impl.proxy.MapProxyImpl
import io.micronaut.cache.SyncCache
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
                "hazelcast.caches.foo.maximumSize": 25
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)

        then:
        hazelcastManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = hazelcastManager.getCache('foo')
        cache.name == 'foo'
        cache.configuration.maximumSize == 25
    }

    void "test multiple caches are created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.maximumSize": 25,
                "hazelcast.caches.bar.maximumSize": 99
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
                "hazelcast.caches.foo.backupCount": 3
        ])

        when:
        HazelcastManager hazelcastManager = ctx.getBean(HazelcastManager)
        SyncCache<IMap> cache = hazelcastManager.getCache('foo')
        IMap nativeCache = cache.nativeCache

        then:
        ((MapProxyImpl) nativeCache).getTotalBackupCount() == 3
    }

}
