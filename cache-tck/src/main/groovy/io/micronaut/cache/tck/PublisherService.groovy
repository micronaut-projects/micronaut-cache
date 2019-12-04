package io.micronaut.cache.tck

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

@Singleton
@CacheConfig('counter')
class PublisherService {

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