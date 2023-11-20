package io.micronaut.cache

import io.micronaut.cache.hazelcast.HazelcastCacheManager
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.testcontainers.containers.GenericContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest
class HazelcastSpec extends Specification implements TestPropertyProvider {

    @AutoCleanup
    @Shared
    GenericContainer<?> hazelcast = new GenericContainer<>("hazelcast/hazelcast:" + System.getProperty("hazelcastVersion"))
            .withExposedPorts(5701)

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    HazelcastCacheManager cacheManager;

    void "simple test"() {
        given:
        BlockingHttpClient client = httpClient.toBlocking();

        when:
        Integer inc = client.retrieve("/inc", Integer.class);

        then:
        inc == 1

        when:
        inc = client.retrieve("/inc", Integer.class);

        then:
        inc == 2

        when:
        Integer get = client.retrieve("/get", Integer.class);

        then:
        get == 2
        cacheManager.getCache("counter").get("test", Integer.class).get() == 2

        when:
        client.exchange("/del");

        then:
        cacheManager.getCache("counter").get("test", Integer.class).empty
    }

    @Override
    Map<String, String> getProperties() {
        if (!hazelcast.running) {
            hazelcast.start()
        }
        ["hazelcast.client.network.addresses": "${hazelcast.host}:${hazelcast.firstMappedPort}"]
    }
}
