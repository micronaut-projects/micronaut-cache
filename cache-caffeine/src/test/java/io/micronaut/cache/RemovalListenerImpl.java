package io.micronaut.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;

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
