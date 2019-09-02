package io.micronaut.cache.ehcache

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class EhcacheConfigurationSpec extends Specification{

    void "it creates cache configurations"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "ehcache.caches.foo.keyType": "java.lang.Long",
                "ehcache.caches.foo.valueType": "java.lang.String"
        ])

        when:
        Collection<EhcacheConfiguration> ehcacheConfigurations = ctx.getBeansOfType(EhcacheConfiguration)

        then:
        ehcacheConfigurations.size() == 1
        ehcacheConfigurations.first().getName() == 'foo'
        ehcacheConfigurations.first().getKeyType() == Long
        ehcacheConfigurations.first().getValueType() == String
    }

}
