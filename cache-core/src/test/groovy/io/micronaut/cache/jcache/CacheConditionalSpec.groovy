package io.micronaut.cache.jcache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.annotation.PutOperations
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.annotation.SingleResult
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration
import java.time.Instant
import java.util.concurrent.CompletableFuture

class CacheConditionalSpec extends Specification {

    @AutoCleanup
    @Shared
    ApplicationContext applicationContext = ApplicationContext.run(
            (JCacheManager.JCACHE_ENABLED): true,
            'spec.name': 'CacheConditionalSpec'
    )

    @Shared
    CacheService cacheService = applicationContext.getBean(CacheService)

    @Shared
    CacheManager cacheManager = applicationContext.getBean(CacheManager)

    void '@Cacheable condition is used for #scenario return'(CacheScenario scenario) {
        given:
        Map<String, String> data = loadCacheableData(scenario)

        when:
        sleep(100)

        then: 'test returns cached value'
        Flux.from(cacheService.publisherValue('test')).blockFirst() == data.test

        and: 'ignore is not cached'
        Flux.from(cacheService.publisherValue('ignore')).blockFirst() != data.ignore

        where:
        scenario << CacheScenario.values()
    }

    void '@CachePut condition is used for #scenario returne'(CacheScenario scenario) {
        given:
        String cacheName = 'cond-service'

        when: 'we put a value for test and ignored'
        loadCachePutData(scenario)

        def cache = cacheManager.getCache(cacheName)

        then: 'test is in the cache'
        cache.containsKey('test')
        if (scenario == CacheScenario.PUBLISHER) {
            assert cache.get('test') == ['foo']
        } else {
            assert cache.get('test') == 'foo'
        }

        and: 'ignore is not in the cache as the name was "ignore"'
        !cache.containsKey('ignore')

        and: 'qux is not in the cache as the value was "ignore"'
        !cache.containsKey('qux')

        and: 'ham is in the cache'
        cache.containsKey('ham')
        if (scenario == CacheScenario.PUBLISHER) {
            assert cache.get('ham') == ['yes']
        } else {
            assert cache.get('ham') == 'yes'
        }

        cleanup:
        cache.removeAll()

        where:
        scenario << CacheScenario.values()
    }

    void 'cacheinvalidate uses the condition'() {
        when: 'we invalidate a value for test and ignored'
        def cache = cacheManager.getCache('cond-service')
        cache.put("ignore", "foo")

        and: 'we put values for all the things'
        cacheService.putValue('test', 'foo')
        cacheService.putValue('ignore', 'bar')
        cacheService.putValue('qux', 'ignore')
        cacheService.putValue('ham', 'yes')

        and:
        cacheService.invalidate('test')
        cacheService.invalidate('ignore')
        cacheService.invalidate('qux')
        cacheService.invalidate('ham')

        then: 'the non-ignored values are removed from the cache'
        !cache.containsKey('test')
        !cache.containsKey('qux')
        !cache.containsKey('ham')

        and: 'the ignored value is still in the cache'
        cache.containsKey('ignore')

        and: 'ignore has the value we set at the start'
        cache.get('ignore') == 'foo'
    }

    void 'multiple puts with a condition are handled'() {
        when: 'we invalidate a value for test and ignored'
        def noIgnoreCache = cacheManager.getCache('cache-no-ignore')
        def allCache = cacheManager.getCache('cache-all')
        def noFooCache = cacheManager.getCache('cache-no-foo')

        cacheService.multiPut('foo')
        cacheService.multiPut('bar')
        cacheService.multiPut('ignore')

        then: 'noIgnoreCache ignores the name "ignore"'
        noIgnoreCache.get('foo') == 'set!'
        noIgnoreCache.get('bar') == 'set!'
        noIgnoreCache.get('ignore') == null

        and: 'allCache does not ignore anything'
        allCache.get('foo') == 'set!'
        allCache.get('bar') == 'set!'
        allCache.get('ignore') == 'set!'

        and: 'noFooCache ignores the name "foo"'
        noFooCache.get('foo') == null
        noFooCache.get('bar') == 'set!'
        noFooCache.get('ignore') == 'set!'
    }

    private Map<String, String> loadCacheableData(CacheScenario scenario) {
        String initialTest
        String initialIgnore
        switch (scenario) {
            case CacheScenario.PUBLISHER:
                initialTest = Flux.from(cacheService.publisherValue('test')).blockFirst()
                initialIgnore = Flux.from(cacheService.publisherValue('ignore')).blockFirst()
                break
            case CacheScenario.FUTURE:
                initialTest = cacheService.futureValue('test').get()
                initialIgnore = cacheService.futureValue('ignore').get()
                break
            case CacheScenario.STRING:
                initialTest = cacheService.getValue('test')
                initialIgnore = cacheService.getValue('ignore')
                break
        }
        return [test: initialTest, ignore: initialIgnore]
    }

    private void loadCachePutData(CacheScenario scenario) {
        switch (scenario) {
            case CacheScenario.PUBLISHER:
                Flux.from(cacheService.publisherPutValue('test', 'foo')).blockFirst()
                Flux.from(cacheService.publisherPutValue('ignore', 'bar')).blockFirst()
                Flux.from(cacheService.publisherPutValue('qux', 'ignore')).blockFirst()
                Flux.from(cacheService.publisherPutValue('ham', 'yes')).blockFirst()
                break
            case CacheScenario.FUTURE:
                cacheService.futurePutValue('test', 'foo').get()
                cacheService.futurePutValue('ignore', 'bar').get()
                cacheService.futurePutValue('qux', 'ignore').get()
                cacheService.futurePutValue('ham', 'yes').get()
                break
            case CacheScenario.STRING:
                cacheService.putValue('test', 'foo')
                cacheService.putValue('ignore', 'bar')
                cacheService.putValue('qux', 'ignore')
                cacheService.putValue('ham', 'yes')
                break
        }
    }

    private enum CacheScenario {
        PUBLISHER,
        FUTURE,
        STRING
    }

    @Factory
    @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
    @Requires(property = "spec.name", value = "CacheConditionalSpec")
    static class CacheFactory {

        @Singleton
        CacheManager cacheManager() {
            Caching.getCachingProvider().cacheManager.tap {
                createCache('cond-service', new MutableConfiguration())
                createCache('cache-no-ignore', new MutableConfiguration())
                createCache('cache-all', new MutableConfiguration())
                createCache('cache-no-foo', new MutableConfiguration())
            }
        }
    }

    @Singleton
    @CacheConfig('cond-service')
    @Requires(property = "spec.name", value = "CacheConditionalSpec")
    static class CacheService {

        @Cacheable(condition = "#{ name != 'ignore' }")
        String getValue(String name) {
            return Instant.now().toString()
        }

        @Cacheable(condition = "#{ name != 'ignore' }")
        CompletableFuture<String> futureValue(String name) {
            CompletableFuture.completedFuture(Instant.now().toString())
        }

        @SingleResult
        @Cacheable(condition = "#{ name != 'ignore' }")
        Publisher<String> publisherValue(String name) {
            Flux.just(Instant.now().toString())
        }

        @CachePut(parameters = 'name', condition = "#{ name != 'ignore' && value != 'ignore' }")
        String putValue(String name, String value) {
            value
        }

        @PutOperations([
                @CachePut(cacheNames = 'cache-no-ignore', condition = "#{ name != 'ignore' }"),
                @CachePut(cacheNames = 'cache-all'),
                @CachePut(cacheNames = 'cache-no-foo', condition = "#{ name != 'foo' }"),
        ])
        String multiPut(String name) {
            return "set!"
        }

        @CachePut(parameters = 'name', condition = "#{ name != 'ignore' && value != 'ignore' }")
        CompletableFuture<String> futurePutValue(String name, String value) {
            CompletableFuture.completedFuture(value)
        }

        @CachePut(parameters = 'name', condition = "#{ name != 'ignore' && value != 'ignore' }")
        Publisher<String> publisherPutValue(String name, String value) {
            Flux.just(value)
        }

        @CacheInvalidate(parameters = 'name', condition = "#{ name != 'ignore' }")
        void invalidate(String name) {
        }
    }
}
