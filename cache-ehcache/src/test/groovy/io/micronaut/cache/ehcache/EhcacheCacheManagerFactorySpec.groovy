package io.micronaut.cache.ehcache

import io.micronaut.context.ApplicationContext
import org.ehcache.CacheManager
import org.ehcache.Status
import org.ehcache.core.spi.service.StatisticsService
import spock.lang.Specification

class EhcacheCacheManagerFactorySpec extends Specification {

    void "it creates a cache manager and initialises it"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(["ehcache.enabled": true])

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        cacheManager.status == Status.AVAILABLE
    }

    void "it creates an statistics service"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(["ehcache.enabled": true])

        when:
        StatisticsService statisticsService = ctx.getBean(StatisticsService)

        then:
        statisticsService
    }
}
