package io.micronaut.discovery.client;

import io.micronaut.cache.discovery.CachingCompositeDiscoveryClient;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class NativeDiscoveryTest {

    @Inject
    CachingCompositeDiscoveryClient cachingCompositeDiscoveryClient;

    @Test
    void test() {
        List<ServiceInstance> instances = Flux.from(cachingCompositeDiscoveryClient.getInstances("test")).blockFirst();
        assertTrue(instances.isEmpty());
    }
}
