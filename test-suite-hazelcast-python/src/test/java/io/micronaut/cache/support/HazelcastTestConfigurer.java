package io.micronaut.cache.support;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

/**
 * Supplies the Hazelcast container address to the Python tests running with the {@value #HAZELCAST_ENVIRONMENT}
 * environment (the {@code TestPropertyProvider} of the Java test runs before the Python runtime exists).
 */
@ContextConfigurer
public class HazelcastTestConfigurer implements ApplicationContextConfigurer {

    public static final String HAZELCAST_ENVIRONMENT = "hazelcast";

    private static GenericContainer<?> container;

    @Override
    public void configure(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (environment.getActiveNames().contains(HAZELCAST_ENVIRONMENT)) {
            environment.addPropertySource(PropertySource.of(HAZELCAST_ENVIRONMENT, getProperties()));
        }
    }

    private static synchronized Map<String, Object> getProperties() {
        if (container == null) {
            container = new GenericContainer<>("hazelcast/hazelcast:" + System.getProperty("hazelcastVersion"))
                .withExposedPorts(5701);
            container.start();
        }
        return Map.of(
            "hazelcast.client.network.addresses", container.getHost() + ":" + container.getFirstMappedPort()
        );
    }
}
