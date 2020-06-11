import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration
import io.micronaut.context.ApplicationContext
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheRebalanceMode
import spock.lang.Specification

class IgniteCacheSpec extends Specification {

    void "test ignite cache instance"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                                      : true,
            "ignite.clients.default.force-return-values"          : true,
            "ignite.clients.default.client-mode"                  : true,
            "ignite.clients.default.discovery.multicast.addresses": ["localhost:47500..47509"],
            "ignite.caches.counter.client"                        : "default",
            "ignite.caches.counter.group-name"                    : "test",
            "ignite.caches.counter.atomicity-mode"                : "ATOMIC",
            "ignite.caches.counter.backups"                       : 4,
            "ignite.caches.counter.default-lock-timeout"          : 5000,
            "ignite.caches.counter.rebalance-mode"                : "NONE"
        ])
        when:
        Collection<IgniteCacheConfiguration> cacheConfiguration = ctx.getBeansOfType(IgniteCacheConfiguration.class)

        then:
        cacheConfiguration != null
        cacheConfiguration.size() == 1
        cacheConfiguration.first().configuration.name == "counter"
        cacheConfiguration.first().configuration.getRebalanceMode() == CacheRebalanceMode.NONE
        cacheConfiguration.first().configuration.getDefaultLockTimeout() == 5000
        cacheConfiguration.first().configuration.getBackups() == 4
        cacheConfiguration.first().configuration.getGroupName() == "test"
        cacheConfiguration.first().configuration.atomicityMode == CacheAtomicityMode.ATOMIC
        cacheConfiguration.first().client == "default"
    }
}
