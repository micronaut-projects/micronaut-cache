package io.micronaut.cache

import io.micronaut.cache.hazelcast.HazelcastClientConfiguration
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton

// tag::clazz[]
@Singleton
class HazelcastAdditionalSettings : BeanCreatedEventListener<HazelcastClientConfiguration> {

    override fun onCreated(event: BeanCreatedEvent<HazelcastClientConfiguration>) = event.bean.apply {
        // Set anything on the configuration
        clusterName = "dev"
    }
}
// end::clazz[]
