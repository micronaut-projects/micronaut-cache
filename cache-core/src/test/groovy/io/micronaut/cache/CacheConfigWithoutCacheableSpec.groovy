package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.jcache.JCacheManager
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration

class CacheConfigWithoutCacheableSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(
            (JCacheManager.JCACHE_ENABLED):true,
            'spec.name': 'CacheConfigWithoutCacheableSpec'
    )

    void "it doesn't cache methods if not explicitly annotated"() {
        given:
        MyService myService = context.getBean(MyService)

        when:
        def things = myService.things().toIterable()

        then:
        things.size() == 3
    }

    @Singleton
    @CacheConfig('my-service')
    @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
    @Requires(property = "spec.name", value = "CacheConfigWithoutCacheableSpec")
    static class MyService {

        private String[] things = ['one', 'two', 'three'].toArray()

        Flux<String> things() {
            Flux.fromArray(things)
        }

    }

    @Factory
    @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
    @Requires(property = "spec.name", value = "CacheConfigWithoutCacheableSpec")
    static class CacheFactory {

        @Singleton
        @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
        CacheManager cacheManager() {
            def cacheManager = Caching.getCachingProvider().cacheManager
            cacheManager.createCache('my-service', new MutableConfiguration())
            return cacheManager
        }
    }

}
