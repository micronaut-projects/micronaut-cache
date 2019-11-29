package io.micronaut.cache.infinispan

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.ApplicationContext
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

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

        cleanup:
        applicationContext.close()
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
