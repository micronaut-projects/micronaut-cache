package io.micronaut.cache.ehcache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.annotation.InvalidateOperations
import io.micronaut.cache.annotation.PutOperations
import io.micronaut.context.ApplicationContext
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger

class EhcacheSyncCacheSpec extends Specification {

    void "test cacheable annotations"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'ehcache.caches.counter.enabled': true,
                'ehcache.caches.counter2.enabled': true,
        )

        when:
        CounterService counterService = applicationContext.getBean(CounterService)

        then:
        counterService.flowableValue("test").blockingFirst() == 0
        counterService.singleValue("test").blockingGet() == 0

        when:
        counterService.reset()
        def result =counterService.increment("test")

        then:
        result == 1
        counterService.flowableValue("test").blockingFirst() == 1
        counterService.futureValue("test").get() == 1
        counterService.stageValue("test").toCompletableFuture().get() == 1
        counterService.singleValue("test").blockingGet() == 1
        counterService.getValue("test") == 1

        when:
        result = counterService.incrementNoCache("test")

        then:
        result == 2
        counterService.flowableValue("test").blockingFirst() == 1
        counterService.futureValue("test").get() == 1
        counterService.stageValue("test").toCompletableFuture().get() == 1
        counterService.singleValue("test").blockingGet() == 1
        counterService.getValue("test") == 1

        when:
        counterService.reset("test")

        then:
        counterService.getValue("test") == 0

        when:
        counterService.reset("test")

        then:
        counterService.futureValue("test").get() == 0
        counterService.stageValue("test").toCompletableFuture().get() == 0

        when:
        counterService.set("test", 3)

        then:
        counterService.getValue("test") == 3
        counterService.futureValue("test").get() == 3
        counterService.stageValue("test").toCompletableFuture().get() == 3

        when:
        result = counterService.increment("test")

        then:
        result == 4
        counterService.getValue("test") == 4
        counterService.futureValue("test").get() == 4
        counterService.stageValue("test").toCompletableFuture().get() == 4

        when:
        result = counterService.futureIncrement("test").get()

        then:
        result == 5
        counterService.getValue("test") == 5
        counterService.futureValue("test").get() == 5
        counterService.stageValue("test").toCompletableFuture().get() == 5

        when:
        counterService.reset()

        then:
        !counterService.getOptionalValue("test").isPresent()
        counterService.getValue("test") == 0
        counterService.getOptionalValue("test").isPresent()
        counterService.getValue2("test") == 0

        when:
        counterService.increment("test")
        counterService.increment("test")

        then:
        counterService.getValue("test") == 2
        counterService.getValue2("test") == 0

        when:
        counterService.increment2("test")

        then:
        counterService.getValue("test") == 1
        counterService.getValue2("test") == 1
    }


    void "test publisher cache methods are not called for hits"() {
        given:
        ApplicationContext ctx = ApplicationContext.run("ehcache.caches.counter.enabled": true)

        PublisherService publisherService = ctx.getBean(PublisherService)

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
    static class CounterService {
        Map<String, Integer> counters = new LinkedHashMap<>()
        Map<String, Integer> counters2 = new LinkedHashMap<>()

        int incrementNoCache(String name) {
            int value = counters.computeIfAbsent(name, { 0 })
            counters.put(name, ++value)
            return value
        }

        @CachePut
        int increment(String name) {
            int value = counters.computeIfAbsent(name, { 0 })
            counters.put(name, ++value)
            return value
        }

        @PutOperations([
                @CachePut('counter'),
                @CachePut('counter2')

        ])
        int increment2(String name) {
            int value = counters2.computeIfAbsent(name, { 0 })
            counters2.put(name, ++value)
            return value
        }

        @Cacheable
        CompletableFuture<Integer> futureValue(String name) {
            return CompletableFuture.completedFuture(counters.computeIfAbsent(name, { 0 }))
        }

        @Cacheable
        CompletionStage<Integer> stageValue(String name) {
            return CompletableFuture.completedFuture(counters.computeIfAbsent(name, { 0 }))
        }

        @Cacheable
        @SingleResult
        Flowable<Integer> flowableValue(String name) {
            return Flowable.just(counters.computeIfAbsent(name, { 0 }))
        }

        @Cacheable
        Single<Integer> singleValue(String name) {
            return Single.just(counters.computeIfAbsent(name, { 0 }))
        }

        @CachePut
        CompletableFuture<Integer> futureIncrement(String name) {
            int value = counters.computeIfAbsent(name, { 0 })
            counters.put(name, ++value)
            return CompletableFuture.completedFuture(value)
        }

        @Cacheable
        int getValue(String name) {
            return counters.computeIfAbsent(name, { 0 })
        }

        @Cacheable('counter2')
        int getValue2(String name) {
            return counters2.computeIfAbsent(name, { 0 })
        }

        @Cacheable
        Optional<Integer> getOptionalValue(String name) {
            return Optional.ofNullable(counters.get(name))
        }

        @CacheInvalidate(all = true)
        void reset() {
            counters.clear()
        }

        @CacheInvalidate
        void reset(String name) {
            counters.remove(name)
        }

        @InvalidateOperations([
                @CacheInvalidate('counter'),
                @CacheInvalidate('counter2')
        ])
        void reset2(String name) {
            counters.remove(name)
        }

        @CacheInvalidate(parameters = 'name')
        void set(String name, int val) {
            counters.put(name, val)
        }
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
