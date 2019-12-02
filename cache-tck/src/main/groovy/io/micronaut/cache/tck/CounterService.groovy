package io.micronaut.cache.tck

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.annotation.InvalidateOperations
import io.micronaut.cache.annotation.PutOperations
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single

import javax.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@CacheConfig('counter')
class CounterService {
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
