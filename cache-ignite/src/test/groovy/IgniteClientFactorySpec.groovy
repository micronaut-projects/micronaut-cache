import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import org.apache.ignite.Ignite
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
@spock.lang.Retry
class IgniteClientFactorySpec extends Specification {
    @Shared
    GenericContainer ignite = new GenericContainer("apacheignite/ignite:2.8.0")
        .withExposedPorts(47500, 47100)

    void "test ignite client instance is created"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
            "ignite.enabled"                                      : true,
            "ignite.clients.default.force-return-values"          : true,
            "ignite.clients.default.client-mode"                  : true,
            "ignite.clients.default.discovery.multicast.addresses": ["localhost:47500..47509"],
            "ignite.clients.other.force-return-values"            : true,
            "ignite.clients.other.client-mode"                    : true,
            "ignite.clients.other.discovery.multicast.addresses"  : ["localhost:47500..47509"]
        ])
        when:
        Ignite defaultInstance = ctx.getBean(Ignite.class, Qualifiers.byName("default"))
        Ignite otherInstance = ctx.getBean(Ignite.class, Qualifiers.byName("other"))

        then:
        defaultInstance != null
        otherInstance != null
        defaultInstance.name() == "default"
        otherInstance.name() == "other"
    }
}
