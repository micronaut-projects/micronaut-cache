package io.micronaut.cache.infinispan;

import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheInfo;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class InfinispanAsyncCache implements AsyncCache<RemoteCache<Object, Object>> {

    private final RemoteCache<Object, Object> nativeCache;
    private final ConversionService<?> conversionService;

    public InfinispanAsyncCache(RemoteCache<Object, Object> nativeCache, ConversionService<?> conversionService) {
        this.nativeCache = nativeCache;
        this.conversionService = conversionService;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        return nativeCache.getAsync(key).thenApply(value -> conversionService.convert(value, ConversionContext.of(requiredType)));
    }

    @Override
    public <T> CompletableFuture<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
        return get(key, requiredType).thenApply(optionalValue -> optionalValue.orElseGet(supplier));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        return nativeCache.putIfAbsentAsync(key, value).thenApply(val -> {
            final T v = (T) val;
            final Class<T> aClass = (Class<T>) value.getClass();
            return conversionService.convert(v, aClass);
        });
    }

    @Override
    public CompletableFuture<Boolean> put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        return nativeCache
                .putAsync(key, value)
                .thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        return nativeCache
                .removeAsync(key)
                .thenApply(Objects::nonNull);
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        return nativeCache
                .clearAsync()
                .handle((aVoid, throwable) -> throwable != null);
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        return Publishers.just(new InfinispanCacheInfo(nativeCache));
    }

    @Override
    public String getName() {
        return nativeCache.getName();
    }

    @Override
    public RemoteCache<Object, Object> getNativeCache() {
        return nativeCache;
    }
}
