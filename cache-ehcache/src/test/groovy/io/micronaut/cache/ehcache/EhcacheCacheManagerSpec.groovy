package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class EhcacheCacheManagerSpec extends Specification {

    void "it create caches from configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
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
        cache.nativeCache
    }

}
