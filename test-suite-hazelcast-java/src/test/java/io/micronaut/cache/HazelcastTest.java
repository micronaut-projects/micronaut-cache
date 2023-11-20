package io.micronaut.cache;

import io.micronaut.cache.hazelcast.HazelcastCacheManager;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Testcontainers
class HazelcastTest implements TestPropertyProvider {

    @Container
    GenericContainer<?> hazelcast = new GenericContainer<>("hazelcast/hazelcast:" + System.getProperty("hazelcastVersion"))
        .withExposedPorts(5701);

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    HazelcastCacheManager cacheManager;

    @Test
    void simpleTest() {
        BlockingHttpClient client = httpClient.toBlocking();

        Integer inc = client.retrieve("/inc", Integer.class);
        assertEquals(1, inc);

        inc = client.retrieve("/inc", Integer.class);
        assertEquals(2, inc);

        Integer get = client.retrieve("/get", Integer.class);
        assertEquals(2, get);

        assertEquals(2, cacheManager.getCache("counter").get("test", Integer.class).get());

        client.exchange("/del");

        assertTrue(cacheManager.getCache("counter").get("test", Integer.class).isEmpty());
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
        return Map.of(
            "hazelcast.client.network.addresses", hazelcast.getHost() + ":" + hazelcast.getFirstMappedPort()
        );
    }
}
