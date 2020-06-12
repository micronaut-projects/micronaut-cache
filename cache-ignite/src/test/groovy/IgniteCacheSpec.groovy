import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration
import io.micronaut.context.ApplicationContext
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheRebalanceMode
import spock.lang.Specification

class IgniteCacheSpec extends Specification {

    void "test ignite cache instance"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                            : true,
            "ignite.clients.default.addresses"          : ["localhost:47500..47509"],
            "ignite.caches.counter.client"              : "default",
            "ignite.caches.counter.group-name"          : "test",
            "ignite.caches.counter.atomicity-mode"      : "ATOMIC",
            "ignite.caches.counter.backups"             : 4,
            "ignite.caches.counter.default-lock-timeout": 5000,
            "ignite.caches.counter.rebalance-mode"      : "NONE"
        ])
        when:
        Collection<IgniteCacheConfiguration> cacheConfiguration = ctx.getBeansOfType(IgniteCacheConfiguration.class)

        then:
        cacheConfiguration != null
        cacheConfiguration.size() == 1
        cacheConfiguration.first().client == "default"
        cacheConfiguration.first().name == "counter"
        cacheConfiguration.first().configuration.getGroupName() == "test"
        cacheConfiguration.first().configuration.getAtomicityMode() == CacheAtomicityMode.ATOMIC
        cacheConfiguration.first().configuration.backups == 4
        cacheConfiguration.first().configuration.defaultLockTimeout == 5000
        cacheConfiguration.first().configuration.rebalanceMode == CacheRebalanceMode.NONE
    }
}
