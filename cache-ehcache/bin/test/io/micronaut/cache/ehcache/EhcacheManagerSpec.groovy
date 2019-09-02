package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
class EhcacheManagerSpec extends Specification {

    void "it create caches from configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "ehcache.caches.foo.keyType": "java.lang.Long",
                "ehcache.caches.foo.valueType": "java.lang.String"
        ])

        when:
        EhcacheManager ehcacheManager = ctx.getBean(EhcacheManager)

        then:
        ehcacheManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = ehcacheManager.getCache('foo')
        cache.nativeCache.runtimeConfiguration.keyType == Long
        cache.nativeCache.runtimeConfiguration.valueType == String
        cache.nativeCache
    }

}
