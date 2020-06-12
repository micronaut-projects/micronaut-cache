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
class IgniteClientAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Shared
    GenericContainer ignite = new GenericContainer("apacheignite/ignite:2.8.0")
        .withExposedPorts(10800)

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run([
            "ignite.enabled"                  : true,
            "ignite.clients.default.addresses": ["127.0.0.1:${ignite.getMappedPort(10800)}"],
            "ignite.caches.counter.client"    : "default",
            "ignite.caches.counter2.client"   : "default",
            "ignite.caches.test.client"       : "default"
        ])
    }
}
