package io.micronaut.cache.ignite;

import io.micronaut.cache.AsyncCache;
import io.micronaut.core.type.Argument;
import org.apache.ignite.IgniteCache;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class IgniteAsyncCache implements AsyncCache<IgniteCache> {
    @Override
    public <T> CompletableFuture<Optional<T>> get(Object key, Argument<T> requiredType) {
        return null;
    }

    @Override
    public <T> CompletableFuture<T> get(Object key, Argument<T> requiredType, Supplier<T> supplier) {
        return null;
    }

    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(Object key, T value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> put(Object key, Object value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> invalidate(Object key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public IgniteCache getNativeCache() {
        return null;
    }
}
