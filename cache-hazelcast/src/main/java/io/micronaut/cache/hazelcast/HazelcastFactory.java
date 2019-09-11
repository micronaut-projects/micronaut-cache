package io.micronaut.cache.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class HazelcastFactory {

    @Singleton
    // pass in cacheConfigurations into here and create mapConfig before doing newHazelcastInstance
    public HazelcastInstance hazelcast(HazelcastConfiguration hazelcastConfiguration) {
        return Hazelcast.newHazelcastInstance(hazelcastConfiguration);
    }
}
