package io.micronaut.cache;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "micronaut.caches.counter.initial-capacity", value = "10")
@Property(name = "micronaut.caches.counter.test-mode", value = "true")
@Property(name = "micronaut.caches.counter.maximum-size", value = "20")
@Property(name = "micronaut.caches.counter.listen-to-removals", value = "true")
class NativeCaffeineTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    BeanContext ctx;

    @Test
    void simpleTest() {
        BlockingHttpClient client = httpClient.toBlocking();
        RemovalListenerImpl bean = ctx.getBean(RemovalListenerImpl.class);

        Integer inc = client.retrieve("/inc", Integer.class);
        assertEquals(1, inc);

        assertTrue(() -> bean.getRemovals().isEmpty());

        inc = client.retrieve("/inc", Integer.class);
        assertEquals(2, inc);

        List<String> expectedEventsAfterReplacement = List.of("test|1|REPLACED");
        assertEquals(expectedEventsAfterReplacement, bean.getRemovals());

        Integer get = client.retrieve("/get", Integer.class);
        assertEquals(2, get);

        client.exchange("/del");

        List<String> expectedEventsAfterRemoval = List.of("test|1|REPLACED", "test|2|EXPLICIT");
        assertEquals(expectedEventsAfterRemoval, bean.getRemovals());
    }
}
