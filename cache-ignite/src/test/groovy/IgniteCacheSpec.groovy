import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration
import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration
import io.micronaut.context.ApplicationContext
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheRebalanceMode
import spock.lang.Specification

class IgniteCacheSpec extends Specification {

    void "test ignite cache disabled"() {
        when:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                  : false,
            "ignite.clients.default.addresses": ["localhost:1080"],
            "ignite.caches.default.client"    : "test",
        ])

        then:
        !ctx.containsBean(IgniteClientConfiguration.class)
        !ctx.containsBean(IgniteCacheConfiguration.class)
    }

    void "test ignite cache instance"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                            : true,
            "ignite.clients.default.addresses"          : ["localhost:1080"],
            "ignite.caches.counter.client"              : "default",
            "ignite.caches.counter.group-name"          : "test",
            "ignite.caches.counter.atomicity-mode"      : "ATOMIC",
            "ignite.caches.counter.backups"             : 4,
            "ignite.caches.counter.default-lock-timeout": 5000,
            "ignite.caches.counter.rebalance-mode"      : "NONE"
        ])
        when:
        Collection<IgniteCacheConfiguration> cacheConfiguration = ctx.getBeansOfType(IgniteCacheConfiguration.class)
        Collection<IgniteClientConfiguration> clientConfigurations = ctx.getBeansOfType(IgniteClientConfiguration.class)

        then:
        cacheConfiguration != null
        clientConfigurations != null
        cacheConfiguration.size() == 1
        clientConfigurations.size() == 1
        clientConfigurations.first().client.addresses.size() == 1
        cacheConfiguration.first().client == "default"
        cacheConfiguration.first().name == "counter"
        clientConfigurations.first().client.addresses.first() == "localhost:1080"
        cacheConfiguration.first().configuration.getGroupName() == "test"
        cacheConfiguration.first().configuration.getAtomicityMode() == CacheAtomicityMode.ATOMIC
        cacheConfiguration.first().configuration.backups == 4
        cacheConfiguration.first().configuration.defaultLockTimeout == 5000
        cacheConfiguration.first().configuration.rebalanceMode == CacheRebalanceMode.NONE
    }
}
