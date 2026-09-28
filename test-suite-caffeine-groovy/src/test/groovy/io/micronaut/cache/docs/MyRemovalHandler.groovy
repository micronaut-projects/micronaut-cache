package io.micronaut.cache.docs

import com.github.benmanes.caffeine.cache.RemovalCause
import jakarta.inject.Singleton

@Singleton
class MyRemovalHandler {

    List<String> removals = []

    void handle(String key, Integer value, RemovalCause cause) {
        removals << "$key|$value|$cause"
    }
}
