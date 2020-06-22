/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.tck

import io.micronaut.cache.annotation.*
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single

import javax.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@Singleton
@CacheConfig('counter')
class AsyncCounterService {
    Map<String, Integer> counters = new LinkedHashMap<>()
    Map<String, Integer> counters2 = new LinkedHashMap<>()

    int incrementNoCache(String name) {
        int value = counters.computeIfAbsent(name, { 0 })
        counters.put(name, ++value)
        return value
    }

    @CachePut(async = true)
    int increment(String name) {
        int value = counters.computeIfAbsent(name, { 0 })
        counters.put(name, ++value)
        return value
    }

    @PutOperations([
            @CachePut(value = 'counter', async = true),
            @CachePut(value = 'counter2', async = true)

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

    @CachePut(async = true)
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

    @CacheInvalidate(all = true, async = true)
    void reset() {
        counters.clear()
    }

    @CacheInvalidate(async = true)
    void reset(String name) {
        counters.remove(name)
    }

    @InvalidateOperations([
            @CacheInvalidate(value = 'counter', async = true),
            @CacheInvalidate(value = 'counter2', async = true)
    ])
    void reset2(String name) {
        counters.remove(name)
    }

    @CacheInvalidate(parameters = 'name', async = true)
    void set(String name, int val) {
        counters.put(name, val)
    }
}
