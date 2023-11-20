package io.micronaut.cache

import io.micronaut.cache.hazelcast.HazelcastClientConfiguration
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton

// tag::clazz[]
@Singleton
class HazelcastAdditionalSettings implements BeanCreatedEventListener<HazelcastClientConfiguration> {

    @Override
    HazelcastClientConfiguration onCreated(@NonNull BeanCreatedEvent<HazelcastClientConfiguration> event) {
        event.bean.tap {
            // Set anything on the configuration
            clusterName = "dev"
        }
    }
}
// end::clazz[]
