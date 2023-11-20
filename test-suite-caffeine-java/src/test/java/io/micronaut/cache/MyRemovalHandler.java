package io.micronaut.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class MyRemovalHandler {

    private List<String> removals = new ArrayList<>();

    void handle(String key, Integer value, RemovalCause cause) {
        removals.add(key + "|" + value + "|" + cause);
    }

    public List<String> getRemovals() {
        return removals;
    }
}
