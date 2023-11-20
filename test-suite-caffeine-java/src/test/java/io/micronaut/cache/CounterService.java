package io.micronaut.cache;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Singleton
@CacheConfig(cacheNames = {"counter"})
public class CounterService {

    Map<String, Integer> counters = new LinkedHashMap<>();

    @CachePut
    public int increment(String name) {
        int value = counters.computeIfAbsent(name, s -> 0);
        counters.put(name, ++value);
        return value;
    }

    @Cacheable
    public int getValue(String name) {
        return counters.computeIfAbsent(name, s -> 0);
    }


    @CacheInvalidate()
    public void reset(String name) {
        counters.remove(name);
    }

    @CacheInvalidate
    public void set(String name, int val) {
        counters.put(name, val);
    }
}
