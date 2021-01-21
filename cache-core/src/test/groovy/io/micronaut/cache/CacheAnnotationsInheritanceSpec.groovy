package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.jcache.JCacheManager
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration
import javax.inject.Singleton

class CacheAnnotationsInheritanceSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(
            (JCacheManager.JCACHE_ENABLED):true,
            'spec.name': 'CacheAnnotationsInheritanceSpec'
    )

    void "cache annotations are inherited"() {
        given:
        MyService myService = context.getBean(MyService)
        myService.reset()

        expect:
        myService.cachedNumber() == 1
        myService.cachedNumber() == 1
        myService.cachedNumber() == 1
        myService.cachedNumber() == 1
    }

    void "non-annotated methods are not cached"() {
        given:
        MyService myService = context.getBean(MyService)
        myService.reset()

        expect:
        myService.nonCachedNumber() == 1
        myService.nonCachedNumber() == 2
        myService.nonCachedNumber() == 3
        myService.nonCachedNumber() == 4
    }


    @CacheConfig('my-service')
    static class MyParentClass {

        private int number = 0

        void reset() {
            number = 0
        }

        @Cacheable
        int cachedNumber() {
            return ++number
        }


        int nonCachedNumber() {
            return ++number
        }

    }

    @Singleton
    @Requires(property = "spec.name", value = "CacheAnnotationsInheritanceSpec")
    static class MyService extends MyParentClass {}

    @Factory
    @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
    @Requires(property = "spec.name", value = "CacheAnnotationsInheritanceSpec")
    static class CacheFactory {

        @Singleton
        @Requires(property = JCacheManager.JCACHE_ENABLED, value = "true")
        javax.cache.CacheManager cacheManager() {
            def cacheManager = Caching.getCachingProvider().cacheManager
            cacheManager.createCache('my-service', new MutableConfiguration())
            return cacheManager
        }
    }

}
