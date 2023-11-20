package io.micronaut.cache;

import io.micronaut.cache.hazelcast.HazelcastClientConfiguration;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;

// tag::clazz[]
@Singleton
public class HazelcastAdditionalSettings implements BeanCreatedEventListener<HazelcastClientConfiguration> {

    @Override
    public HazelcastClientConfiguration onCreated(@NonNull BeanCreatedEvent<HazelcastClientConfiguration> event) {
        HazelcastClientConfiguration configuration = event.getBean();
        // Set anything on the configuration
        configuration.setClusterName("dev");

        return configuration;
    }
}
// end::clazz[]
