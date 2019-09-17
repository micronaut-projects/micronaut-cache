package io.micronaut.cache.hazelcast;

import com.hazelcast.core.ExecutionCallback;
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
        CompletableFuture<T> future = new CompletableFuture<>();
        nativeCache.getAsync(key).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                if (response != null) {
                    future.complete(conversionService.convert(response, ConversionContext.of(requiredType)).get());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.complete(supplier.get());
            }
        });
        return future;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        final T v = (T) nativeCache.putIfAbsent(key, value);
        final Class<T> aClass = (Class<T>) value.getClass();
        future.complete(conversionService.convert(v, aClass));
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        nativeCache.putAsync(key, value).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                if (response != null) {
                    future.complete(true);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        nativeCache.removeAsync(key).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                if (response != null) {
                    future.complete(true);
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
    public CompletableFuture<Boolean> invalidateAll() {
        nativeCache.clear();
        return CompletableFuture.completedFuture(true);
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
