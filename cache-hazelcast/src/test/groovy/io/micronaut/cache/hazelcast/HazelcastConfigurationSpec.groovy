package io.micronaut.cache.hazelcast

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class HazelcastConfigurationSpec extends Specification{

    void "it creates cache configurations"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.maximumSize": 25
        ])

        when:
        Collection<HazelcastConfiguration> hazelcastConfigurations = ctx.getBeansOfType(HazelcastConfiguration)

        then:
        hazelcastConfigurations.size() == 1
        hazelcastConfigurations.first().getName() == 'foo'
        hazelcastConfigurations.first().getMaximumSize() == 25
    }

}
