package io.micronaut.cache.jcache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
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

    def cleanup() {
        cacheManager.cacheNames.each { cacheManager.getCache(it).clear() }
    }

    void 'cacheable condition is used'() {
        when:
        def initialTest = cacheService.getValue('test')
        def initialIgnore = cacheService.getValue('ignore')
        sleep(100)

        then: 'test returns cached value'
        cacheService.getValue('test') == initialTest

        and: 'ignore is not cached'
        cacheService.getValue('ignore') != initialIgnore
    }

    void 'cacheable future condition is used'() {
        when:
        def initialTest = cacheService.futureValue('test').get()
        def initialIgnore = cacheService.futureValue('ignore').get()
        sleep(100)

        then: 'test returns cached value'
        cacheService.futureValue('test').get() == initialTest

        and: 'ignore is not cached'
        cacheService.futureValue('ignore').get() != initialIgnore
    }

    void 'cacheable publisher condition is used'() {
        when:
        def initialTest = Flux.from(cacheService.publisherValue('test')).blockFirst()
        def initialIgnore = Flux.from(cacheService.publisherValue('ignore')).blockFirst()
        sleep(100)

        then: 'test returns cached value'
        Flux.from(cacheService.publisherValue('test')).blockFirst() == initialTest

        and: 'ignore is not cached'
        Flux.from(cacheService.publisherValue('ignore')).blockFirst() != initialIgnore
    }

    void 'cacheput uses the condition'() {
        when: 'we put a value for test and ignored'
        cacheService.putValue('test', 'foo')
        cacheService.putValue('ignore', 'bar')
        cacheService.putValue('qux', 'ignore')
        cacheService.putValue('ham', 'yes')
        CacheManager cacheManager = applicationContext.getBean(CacheManager)
        def cache = cacheManager.getCache('cond-service')

        then: 'test is in the cache'
        cache.containsKey('test')
        cache.get('test') == 'foo'

        and: 'ignore is not in the cache as the name was "ignore"'
        !cache.containsKey('ignore')

        and: 'qux is not in the cache as the value was "ignore"'
        !cache.containsKey('qux')

        and: 'ham is in the cache'
        cache.containsKey('ham')
        cache.get('ham') == 'yes'
    }

    void 'future cacheput uses the condition'() {
        when: 'we put a value for test and ignored'
        cacheService.futurePutValue('test', 'foo').get()
        cacheService.futurePutValue('ignore', 'bar').get()
        cacheService.futurePutValue('qux', 'ignore').get()
        cacheService.futurePutValue('ham', 'yes').get()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)
        def cache = cacheManager.getCache('cond-service')

        then: 'test is in the cache'
        cache.containsKey('test')
        cache.get('test') == 'foo'

        and: 'ignore is not in the cache as the name was "ignore"'
        !cache.containsKey('ignore')

        and: 'qux is not in the cache as the value was "ignore"'
        !cache.containsKey('qux')

        and: 'ham is in the cache'
        cache.containsKey('ham')
        cache.get('ham') == 'yes'
    }

    void 'publisher cacheput uses the condition'() {
        when: 'we put a value for test and ignored'
        Flux.from(cacheService.publisherPutValue('test', 'foo')).blockFirst()
        Flux.from(cacheService.publisherPutValue('ignore', 'bar')).blockFirst()
        Flux.from(cacheService.publisherPutValue('qux', 'ignore')).blockFirst()
        Flux.from(cacheService.publisherPutValue('ham', 'yes')).blockFirst()

        def cache = cacheManager.getCache('cond-service')

        then: 'test is in the cache'
        cache.containsKey('test')
        cache.get('test') == ['foo']

        and: 'ignore is not in the cache as the name was "ignore"'
        !cache.containsKey('ignore')

        and: 'qux is not in the cache as the value was "ignore"'
        !cache.containsKey('qux')

        and: 'ham is in the cache'
        cache.containsKey('ham')
        cache.get('ham') == ['yes']
    }

    void 'cacheinvalidate uses the condition'() {
        when: 'we invalidate a value for test and ignored'
        CacheManager cacheManager = applicationContext.getBean(CacheManager)
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

    @Factory
    @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
    @Requires(property = "spec.name", value = "CacheConditionalSpec")
    static class CacheFactory {

        @Singleton
        CacheManager cacheManager() {
            Caching.getCachingProvider().cacheManager.tap {
                createCache('cond-service', new MutableConfiguration())
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
