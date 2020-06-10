import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.IgnoreIf
import spock.lang.Retry
import spock.lang.Shared

@Testcontainers
@Retry
@IgnoreIf({System.getenv('GITHUB_WORKFLOW')})
class IgniteAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    GenericContainer ignite = new GenericContainer("apacheignite/ignite")
        .withExposedPorts(47500)
        .withEnv("CONFIG_URI", "https://raw.githubusercontent.com/apache/ignite/master/examples/config/example-cache.xml")

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run([
            "ignite.clients.force-return-values": true,
            "ignite.clients.default.host": "localhost:47500..47509",
            "ignite.clients.default.client-mode": true,
            "ignite.clients.default.finder.addresses": ["127.0.0.1:47500..47509"]
        ])
    }
}
