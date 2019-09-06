package io.micronaut.cache.hazelcast

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
        HazelcastManager ehcacheManager = ctx.getBean(HazelcastManager)

        then:
        ehcacheManager.cacheNames == ['foo'].toSet()

        and:
        SyncCache cache = ehcacheManager.getCache('foo')
    }

}
