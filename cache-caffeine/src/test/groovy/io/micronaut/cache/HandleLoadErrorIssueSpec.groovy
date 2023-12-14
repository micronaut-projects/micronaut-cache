package io.micronaut.cache

import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.interceptor.ParametersKey
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.convert.ConversionService
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

class HandleLoadErrorIssueSpec extends Specification {

    void "check #type"() {
        given: 'our cache'
        ApplicationContext ctx = ApplicationContext.run(
                'spec.name': 'HandleLoadErrorIssueSpec',
                'spec.type': type,
                'micronaut.caches.cache.initialCapacity':10,
        )
        def cacheManager = ctx.getBean(CacheManager)
        def cache = cacheManager.getCache('cache')

        and: 'a converter that throws an exception when converting poison to Long'
        def triggered = new AtomicBoolean(false)

        ctx.getBean(ConversionService).addConverter(Poison.class, Long.class, (Poison poison) -> {
            triggered.set(true)
            throw new RuntimeException("Poison!")
        } as Function<Poison, Long>)

        and: 'we add poison to the cache'
        cache.put(ParametersKey.ZERO_ARG_KEY, new Poison())

        when: 'we call the service'
        def result = getter(ctx.getBean(MyService))

        then: 'the converter was triggered'
        triggered.get()

        and: 'the original method is invoked'
        result == 666L

        cleanup:
        cache.invalidateAll()
        ctx.close()

        where:
        type    | getter
        'sync'  | { MyService s -> s.test() }
        'async' | { MyService s -> s.test().get() }
    }

    @Introspected
    static class Poison implements Serializable {
    }

    static interface MyService<T> {

        T test()
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'HandleLoadErrorIssueSpec')
    @Requires(property = 'spec.type', value = 'sync')
    static class MySyncService implements MyService<Long> {

        @Cacheable('cache')
        Long test() {
            666L
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'HandleLoadErrorIssueSpec')
    @Requires(property = 'spec.type', value = 'async')
    static class MyAsyncService implements MyService<CompletableFuture<Long>> {

        @Cacheable('cache')
        CompletableFuture<Long> test() {
            CompletableFuture.supplyAsync { 666L }
        }
    }

    @Primary
    @Singleton
    @Replaces(DefaultCacheErrorHandler)
    @Requires(property = 'spec.name', value = 'HandleLoadErrorIssueSpec')
    static class MyCacheErrorHandler implements CacheErrorHandler {

        @Override
        boolean handleLoadError(Cache<?> cache, Object key, RuntimeException e) {
            false
        }
    }
}
