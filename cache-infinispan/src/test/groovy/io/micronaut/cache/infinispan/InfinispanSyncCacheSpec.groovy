package io.micronaut.cache.infinispan

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.ApplicationContext
import io.micronaut.core.async.annotation.SingleResult
import io.micronaut.core.io.socket.SocketUtils
import io.reactivex.Flowable
import io.reactivex.Single
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfiguration
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.configuration.global.GlobalStateConfigurationBuilder
import org.infinispan.configuration.global.TransportConfigurationBuilder
import org.infinispan.globalstate.ConfigurationStorage
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.manager.EmbeddedCacheManager
import org.infinispan.remoting.transport.Transport
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler
import org.infinispan.server.hotrod.HotRodServer
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Singleton
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
class InfinispanSyncCacheSpec extends Specification implements EmbeddedHotRodServerSupport {

    void "test publisher cache methods are not called for hits"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run()
        PublisherService publisherService = applicationContext.getBean(PublisherService)

        expect:
        publisherService.callCount.get() == 0

        when:
        publisherService.flowableValue("abc").blockingFirst()

        then:
        publisherService.callCount.get() == 1

        when:
        publisherService.flowableValue("abc").blockingFirst()

        then:
        publisherService.callCount.get() == 1

        when:
        publisherService.singleValue("abcd").blockingGet()

        then:
        publisherService.callCount.get() == 2

        when:
        publisherService.singleValue("abcd").blockingGet()

        then:
        publisherService.callCount.get() == 2
    }

    @Singleton
    @CacheConfig('counter')
    static class PublisherService {

        AtomicInteger callCount = new AtomicInteger()

        @Cacheable
        @SingleResult
        Flowable<Integer> flowableValue(String name) {
            callCount.incrementAndGet()
            return Flowable.just(0)
        }

        @Cacheable
        Single<Integer> singleValue(String name) {
            callCount.incrementAndGet()
            return Single.just(0)
        }

    }

}
