package io.micronaut.cache.infinispan

import io.micronaut.cache.CacheInfo
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import io.reactivex.Flowable
import org.infinispan.client.hotrod.RemoteCache
import spock.lang.Specification

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
class InfinispanCacheInfoSpec extends Specification implements EmbeddedHotRodServerSupport {

    void "it publishes cache info stats"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'infinispan.client.hotrod.statistics.enabled': true,
                'infinispan.client.hotrod.statistics.jmx-enabled': true,
                'infinispan.client.hotrod.statistics.jmx-domain': 'org.infinispan'
        ])
        InfinispanCacheManager cacheManager = applicationContext.getBean(InfinispanCacheManager)
        SyncCache<RemoteCache<Object, Object>> cache = cacheManager.getCache("test")

        when:
        CacheInfo cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()
        println cacheInfo.get()

        then:
        cacheInfo.get()['implementationClass'] == 'org.infinispan.client.hotrod.impl.RemoteCacheImpl'
//        cacheInfo.get()['infinispan'] == 'org.infinispan.client.hotrod.impl.RemoteCacheImpl'
    }
}
