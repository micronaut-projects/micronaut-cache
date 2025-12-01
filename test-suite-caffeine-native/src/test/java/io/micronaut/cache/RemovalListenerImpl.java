package io.micronaut.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class RemovalListenerImpl implements RemovalListener<String, Integer> {

    private List<String> removals = new ArrayList<>();

    @Override
    public void onRemoval(@Nullable String key, @Nullable Integer value, @NonNull RemovalCause cause) {
        removals.add(key + "|" + value + "|" + cause);
    }

    public List<String> getRemovals() {
        return removals;
    }
}
