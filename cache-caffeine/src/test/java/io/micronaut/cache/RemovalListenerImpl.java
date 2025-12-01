package io.micronaut.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

@Singleton
public class RemovalListenerImpl implements RemovalListener<String, Integer> {

    private boolean removed = false;

    @Override
    public void onRemoval(@Nullable String key, @Nullable Integer value, @NonNull RemovalCause cause) {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }
}
