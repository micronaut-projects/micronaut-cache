import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class CacheManagerSpec extends Specification{
    void "it create caches from configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.caches.test.name": "test"
        ])
        when:
        Collection<IgniteCacheConfiguration> igniteCacheConfigurations = ctx.getBeansOfType(IgniteCacheConfiguration.class)
        then:
        igniteCacheConfigurations.size() == 1
        igniteCacheConfigurations.find().name == "test"
    }

}
