/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache

import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.ApplicationContext
import io.micronaut.core.async.annotation.SingleResult
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import spock.lang.Issue
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class CachePublisherSpec extends Specification {

    @Issue(["https://github.com/micronaut-projects/micronaut-core/issues/1197",
        "https://github.com/micronaut-projects/micronaut-core/issues/1082"])
    void "test cache result of publisher"() {
        given:
        def ctx = ApplicationContext.run(
            'micronaut.caches.num-cache.maximum-size': 10,
            'micronaut.caches.mult-cache.maximum-size': 10
        )
        HelloService helloService = ctx.getBean(HelloService)

        when:
        def publisher = Mono.from(helloService.calculateValue(10))
        def single = helloService.multiplyValue(2)
        def invalidate = helloService.multiplyInvalidate(2)

        then:
        publisher.block() == "Hello 1: 10"
        publisher.block() == "Hello 1: 10"
        single.block() == 4
        invalidate.block() == 4

        when:
        helloService.error().block()

        then:
        def e = thrown(RuntimeException)
        e.message == 'Bad things'

        cleanup:
        ctx.close()
    }

    @Issue(["https://github.com/micronaut-projects/micronaut-cache/issues/363"])
    void "test cache invalidation"() {
        given:
        def ctx = ApplicationContext.run(
            'micronaut.caches.num-cache.maximum-size': 10,
            'micronaut.caches.mult-cache.maximum-size': 10
        )
        HelloService helloService = ctx.getBean(HelloService)

        when:
        def publisher = Mono.from(helloService.calculateValue(10))

        then:
        publisher.block() == "Hello 1: 10"
        publisher.block() == "Hello 1: 10"

        when:
        Mono.from(helloService.invalidateCache(10)).block()

        then:
        publisher.block() == "Hello 2: 10"
        publisher.block() == "Hello 2: 10"

        when:
        Mono.from(helloService.invalidateCacheAsync(10)).block()

        then:
        publisher.block() == "Hello 3: 10"
    }

    @Singleton
    static class HelloService {

        AtomicInteger invocations = new AtomicInteger(0)

        @Cacheable("num-cache")
        @SingleResult
        Publisher<String> calculateValue(Integer num) {
            return Mono.fromCallable({ ->
                def n = invocations.incrementAndGet()
                println("Calculating value for $num")
                return "Hello $n: $num".toString()
            })
        }

        @CacheInvalidate("num-cache")
        Mono<Void> invalidateCache(Integer num) {
            return Mono.empty();
        }

        @CacheInvalidate(value = "num-cache", async = true)
        Mono<Void> invalidateCacheAsync(Integer num) {
            return Mono.empty();
        }

        @CachePut("mult-cache")
        Mono<Integer> multiplyValue(Integer num) {
            Mono.<Integer> create({ MonoSink<Integer> monoSink ->
                monoSink.success(num * 2)
            })
        }

        @CacheInvalidate("mult-cache")
        Mono<Integer> multiplyInvalidate(Integer num) {
            Mono.<Integer> create({ MonoSink<Integer> monoSink ->
                monoSink.success(num * 2)
            })
        }

        @Cacheable("error-cache")
        Mono<String> error() {
            return Mono.error(new RuntimeException("Bad things"))
        }
    }
}
