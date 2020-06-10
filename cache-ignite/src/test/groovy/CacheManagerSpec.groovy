import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class CacheManagerSpec extends Specification{
//    void "it create caches from configuration"() {
//        given:
//        ApplicationContext ctx = ApplicationContext.run([
//            "ignite.caches.test.name": "test"
//        ])
//        when:
//        Collection<IgniteCacheConfiguration> igniteCacheConfigurations = ctx.getBeansOfType(IgniteCacheConfiguration.class)
//        then:
//        igniteCacheConfigurations.size() == 1
//        igniteCacheConfigurations.find().name == "test"
//    }

    void "test configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.enabled" : true,
            "ignite.clients.default.force-return-values": true,
            "ignite.clients.default.client-mode": true,
            "ignite.clients.default.discovery.multicast.addresses": ["localhost:47500..47509"],
        ])
        when:
        Collection<IgniteClientConfiguration> igniteCacheConfigurations = ctx.getBeansOfType(IgniteClientConfiguration.class)
        then:
        igniteCacheConfigurations.size() == 1
        igniteCacheConfigurations.find().configuration.getIgniteInstanceName() == "test"
    }

}
