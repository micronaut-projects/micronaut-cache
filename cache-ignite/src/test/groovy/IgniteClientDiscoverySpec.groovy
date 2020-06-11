import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class IgniteClientDiscoverySpec extends Specification {

    void "test ignite discovery multicast"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.enable"                                                      : true,
            "ignite.clients.default.force-return-values"                         : true,
            "ignite.clients.default.client-mode"                                 : true,
            "ignite.clients.default.discovery.multicast.addresses"               : ["localhost:47500..47509"],
            "ignite.clients.default.discovery.multicast.address-request-attempts": 10,
            "ignite.clients.default.discovery.multicast.response-wait-time"      : 20,
            "ignite.clients.default.discovery.multicast.multicast-port"          : 5000,
            "ignite.clients.default.discovery.multicast.multicast-group"         : "group1"
        ])
        when:
        Collection<IgniteClientConfiguration> clientConfigs = ctx.getBeansOfType(IgniteClientConfiguration.class)

        then:
        clientConfigs.size() == 1
        clientConfigs.first().discovery.multicast != null
        clientConfigs.first().discovery.filesystem == null
        clientConfigs.first().discovery.vm == null
        clientConfigs.first().discovery.multicast.configuration.getAddressRequestAttempts() == 10
        clientConfigs.first().discovery.multicast.configuration.getResponseWaitTime() == 20
        clientConfigs.first().discovery.multicast.configuration.multicastPort == 5000
        clientConfigs.first().discovery.multicast.configuration.multicastGroup == "group1"
    }

    void "test ignite discovery virtual-machine"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.enable"                                : true,
            "ignite.clients.default.force-return-values"   : true,
            "ignite.clients.default.client-mode"           : true,
            "ignite.clients.default.discovery.vm.shared"   : true,
            "ignite.clients.default.discovery.vm.addresses": ["localhost:47500..47509"],
        ])
        when:
        Collection<IgniteClientConfiguration> clientConfigs = ctx.getBeansOfType(IgniteClientConfiguration.class)

        then:
        clientConfigs.size() == 1
        clientConfigs.first().discovery.filesystem == null
        clientConfigs.first().discovery.vm != null
        clientConfigs.first().discovery.multicast == null
        clientConfigs.first().discovery.vm.configuration.shared

    }

    void "test ignite discovery filesystem"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.enable"                                     : true,
            "ignite.clients.default.force-return-values"        : true,
            "ignite.clients.default.client-mode"                : true,
            "ignite.clients.default.discovery.filesystem.shared": true,
            "ignite.clients.default.discovery.filesystem.path"  : "/var/test/path",
        ])

        when:
        Collection<IgniteClientConfiguration> clientConfigs = ctx.getBeansOfType(IgniteClientConfiguration.class)

        then:
        clientConfigs.size() == 1
        clientConfigs.first().discovery.multicast == null
        clientConfigs.first().discovery.filesystem != null
        clientConfigs.first().discovery.vm == null
        clientConfigs.first().discovery.filesystem.configuration.getPath() == "/var/test/path"

    }
}
