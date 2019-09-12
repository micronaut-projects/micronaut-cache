package io.micronaut.cache.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class HazelcastClientFactory {

    @Singleton
    public HazelcastInstance hazelcastClientInstance(HazelcastClientConfiguration hazelcastClientConfiguration) {
        return HazelcastClient.newHazelcastClient(hazelcastClientConfiguration);
    }
}
