package io.micronaut.cache.tck

import io.micronaut.cache.CacheManager
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import spock.lang.Specification

abstract class AbstractStaticCacheManagerSpec extends Specification {

    abstract ApplicationContext createApplicationContext()

    void "test exception is thrown if non configured cache is retrieved"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        when:
        cacheManager.getCache("fooBar")

        then:
        def ex = thrown(ConfigurationException)
        ex.message == "No cache configured for name: fooBar"

        cleanup:
        applicationContext.stop()
    }

}
