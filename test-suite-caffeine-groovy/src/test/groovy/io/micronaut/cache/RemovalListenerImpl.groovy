package io.micronaut.cache

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton

// tag::clazz[]
@Singleton
class RemovalListenerImpl implements RemovalListener<String, Integer> {

    private final MyRemovalHandler handler

    RemovalListenerImpl(MyRemovalHandler handler) {
        this.handler = handler
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable Integer value, @NonNull RemovalCause cause) {
        handler.handle(key, value, cause)
    }
}
// end::clazz[]
