package io.micronaut.cache.infinispan

import io.micronaut.cache.CacheInfo
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
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
        SyncCache<RemoteCache<Object, Object>> cache = cacheManager.getCache("InfinispanCacheInfoSpec")
        InfinispanHotRodClientConfiguration configuration = applicationContext.getBean(InfinispanHotRodClientConfiguration)

        expect:
        configuration.statistics.create().enabled()
        configuration.statistics.create().jmxEnabled()
        configuration.statistics.create().jmxDomain() == "org.infinispan"

        when:
        CacheInfo cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()

        then:
        cacheInfo.get()['implementationClass'] == 'org.infinispan.client.hotrod.impl.RemoteCacheImpl'
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 0
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 0

        when:
        cache.put("foo", "bar")

        then:
        cache.get("foo", Argument.of(String)).get() == "bar"

        when:
        cacheInfo = Flowable.fromPublisher(cache.cacheInfo).blockingFirst()

        then:
        cacheInfo.get()['infinispan']['clientStatistics']['remoteStores'] == 1
        cacheInfo.get()['infinispan']['clientStatistics']['remoteHits'] == 1


        cleanup:
        applicationContext.close()
    }
}
