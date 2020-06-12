import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration
import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration
import io.micronaut.context.ApplicationContext
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheRebalanceMode
import org.apache.ignite.client.SslMode
import org.apache.ignite.transactions.TransactionConcurrency
import org.apache.ignite.transactions.TransactionIsolation
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

    void "test ignite client configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                                                  : true,
            "ignite.clients.default.addresses"                                : ["localhost:1080"],
            "ignite.clients.default.ssl-mode"                                 : "REQUIRED",
            "ignite.clients.default.ssl-client-certificate-key-store-password": "password",
            "ignite.clients.default.timeout"                                  : 5000,
            "ignite.clients.default.send-buffer-size"                         : 200
        ])
        when:
        IgniteClientConfiguration clientConfiguration = ctx.getBean(IgniteClientConfiguration.class)

        then:
        clientConfiguration != null
        clientConfiguration.client.sslMode == SslMode.REQUIRED
        clientConfiguration.client.sslClientCertificateKeyStorePassword == "password"
        clientConfiguration.client.timeout == 5000
        clientConfiguration.client.sendBufferSize == 200
    }

    void "test ignite client transaction configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            "ignite.enabled"                                                         : true,
            "ignite.clients.default.addresses"                                       : ["localhost:1080"],
            "ignite.clients.default.transaction-configuration.default-tx-isolation"  : "REPEATABLE_READ",
            "ignite.clients.default.transaction-configuration.default-tx-concurrency": "PESSIMISTIC",
            "ignite.clients.default.transaction-configuration.default-tx-timeout"    : 5000,
        ])
        when:
        IgniteClientConfiguration clientConfiguration = ctx.getBean(IgniteClientConfiguration.class)

        then:
        clientConfiguration != null
        clientConfiguration.transactionConfiguration.defaultTxIsolation == TransactionIsolation.REPEATABLE_READ
        clientConfiguration.transactionConfiguration.defaultTxTimeout == 5000
        clientConfiguration.transactionConfiguration.defaultTxConcurrency == TransactionConcurrency.PESSIMISTIC
    }

    void "test ignite cache configuration"() {
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
