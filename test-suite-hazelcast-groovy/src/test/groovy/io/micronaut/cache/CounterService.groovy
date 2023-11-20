package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
@CacheConfig(cacheNames = ["counter"])
class CounterService {

    Map<String, Integer> counters = [:]

    @CachePut
    int increment(String name) {
        int value = counters.computeIfAbsent(name, s -> 0)
        counters.put(name, ++value)
        value
    }

    @Cacheable
    int getValue(String name) {
        counters.computeIfAbsent(name, s -> 0)
    }


    @CacheInvalidate()
    void reset(String name) {
        counters.remove(name)
    }

    @CacheInvalidate
    void set(String name, int val) {
        counters.put(name, val)
    }
}
