package io.micronaut.cache.hazelcast;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import io.micronaut.cache.AsyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HazelcastAsyncCache implements AsyncCache<IMap<Object, Object>> {

    private final ConversionService<?> conversionService;
    private final IMap<Object, Object> nativeCache;

    public HazelcastAsyncCache(ConversionService<?> conversionService, IMap<Object, Object> nativeCache) {
        this.conversionService = conversionService;
        this.nativeCache = nativeCache;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        nativeCache.getAsync(key).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                if (response != null) {
                    future.complete(conversionService.convert(response, ConversionContext.of(requiredType)));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public <T> CompletableFuture<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        return get(key, requiredType).orElseGet(supplier);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        final T v = (T) nativeCache.putIfAbsent(key, value);
        final Class<T> aClass = (Class<T>) value.getClass();
        return conversionService.convert(v, aClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        nativeCache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        nativeCache.remove(key);
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        nativeCache.clear();
    }

    @Override
    public String getName() {
        return nativeCache.getName();
    }

    @Override
    public IMap getNativeCache() {
        return nativeCache;
    }
}
