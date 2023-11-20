package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
@CacheConfig(cacheNames = ["counter"])
open class CounterService {

    private val counters: MutableMap<String, Int> = mutableMapOf()

    @CachePut
    open fun increment(name: String): Int {
        var value = counters.computeIfAbsent(name) { s: String? -> 0 }
        counters[name] = ++value
        return value
    }

    @Cacheable
    open fun getValue(name: String) = counters.computeIfAbsent(name) { s: String? -> 0 }

    @CacheInvalidate
    open fun reset(name: String) = counters.remove(name)

    @CacheInvalidate
    open operator fun set(name: String, value: Int) {
        counters[name] = value
    }
}
