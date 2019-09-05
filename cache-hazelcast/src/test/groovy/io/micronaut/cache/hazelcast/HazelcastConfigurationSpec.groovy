package io.micronaut.cache.hazelcast

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class HazelcastConfigurationSpec extends Specification{

    void "test creates multiple cache configurations"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "hazelcast.caches.foo.maximumSize": 25,
                "hazelcast.caches.bar.maximumSize": 99
        ])

        when:
        Collection<HazelcastConfiguration> hazelcastConfigurations = ctx.getBeansOfType(HazelcastConfiguration)

        then:
        hazelcastConfigurations.size() == 2
        hazelcastConfigurations.first().getName() == 'bar'
        hazelcastConfigurations.first().getMaximumSize() == 99
        hazelcastConfigurations.getAt(1).getName() == 'foo'
        hazelcastConfigurations.getAt(1).getMaximumSize() == 25
    }

}
