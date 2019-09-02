package io.micronaut.cache.ehcache;

import io.micronaut.cache.SyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import org.ehcache.Cache;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class EhcacheSyncCache implements SyncCache<Cache> {

    private final ConversionService<?> conversionService;
    private final EhcacheConfiguration configuration;
    private final Cache nativeCache;


    public EhcacheSyncCache(ConversionService<?> conversionService, EhcacheConfiguration configuration, Cache nativeCache) {
        this.conversionService = conversionService;
        this.configuration = configuration;
        this.nativeCache = nativeCache;
    }

    public EhcacheConfiguration getConfiguration() {
        return configuration;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> Optional<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        Object value = nativeCache.get(key);
        if (value != null) {
            return conversionService.convert(value, ConversionContext.of(requiredType));
        }
        return Optional.empty();
    }

    @Override
    public <T> T get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        return get(key, requiredType).orElseGet(supplier);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> Optional<T> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
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
    public void invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        nativeCache.remove(key);
    }

    @Override
    public void invalidateAll() {
        nativeCache.clear();
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public Cache getNativeCache() {
        return nativeCache;
    }


}
