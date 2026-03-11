/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.oracle;

import io.micronaut.cache.CacheInfo;
import io.micronaut.cache.SyncCache;
import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration;
import io.micronaut.cache.oracle.persistence.CacheEntryEntity;
import io.micronaut.cache.oracle.persistence.CacheEntryId;
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository;
import io.micronaut.cache.oracle.serialization.OracleCacheKey;
import io.micronaut.cache.oracle.serialization.OracleKeySerializer;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;
import io.micronaut.json.JsonMapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Synchronous Oracle cache implementation.
 */
public final class OracleSyncCache implements SyncCache<OracleCacheEntryRepository> {

    private static final long CLEANUP_BATCH_SIZE = 100L;

    private final OracleCacheConfiguration configuration;
    private final OracleCacheEntryRepository entryRepository;
    private final OracleKeySerializer keySerializer;
    private final JsonMapper jsonMapper;

    public OracleSyncCache(OracleCacheConfiguration configuration,
                           OracleCacheEntryRepository entryRepository,
                           OracleKeySerializer keySerializer,
                           JsonMapper jsonMapper) {
        this.configuration = configuration;
        this.entryRepository = entryRepository;
        this.keySerializer = keySerializer;
        this.jsonMapper = jsonMapper;
    }

    @NonNull
    @Override
    public <T> Optional<T> get(@NonNull Object key, @NonNull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("requiredType", requiredType);
        return getBySerializedKey(keySerializer.serialize(key), requiredType);
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("requiredType", requiredType);
        ArgumentUtils.requireNonNull("supplier", supplier);

        OracleCacheKey cacheKey = keySerializer.serialize(key);
        Optional<T> existing = getBySerializedKey(cacheKey, requiredType);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (!configuration.isBlocking()) {
            T supplied = supplier.get();
            put(key, supplied);
            return supplied;
        }

        return loadWithDatabaseCoordination(cacheKey, requiredType, supplier);
    }

    @NonNull
    @Override
    public <T> Optional<T> putIfAbsent(@NonNull Object key, @NonNull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);

        @SuppressWarnings("unchecked")
        Argument<T> argument = (Argument<T>) Argument.of(value.getClass());
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        Instant now = Instant.now();

        CacheEntryEntity entity = new CacheEntryEntity();
        entity.setId(new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash()));
        entity.setKeyPayload(cacheKey.getKeyPayload());
        entity.setCreatedAt(now);
        entity.setLastAccessAt(now);
        entity.setExpiresAt(computeExpiry(now, now).orElse(null));
        entity.setValueWeight(1L);
        entity.setValuePayload(encodeValue(value));

        try {
            entryRepository.save(entity);
            return Optional.empty();
        } catch (RuntimeException e) {
            if (!isDuplicateKeyViolation(e)) {
                throw e;
            }
            Optional<CacheEntryEntity> existing = entryRepository.findByIdCacheNameAndIdKeyHash(
                configuration.getCacheName(),
                cacheKey.getKeyHash()
            );
            if (existing.isPresent() && !isExpired(existing.get(), Instant.now()) && hasStoredValue(existing.get().getValuePayload())) {
                Instant access = Instant.now();
                Instant nextExpiry = computeExpiry(access, existing.get().getCreatedAt()).orElse(existing.get().getExpiresAt());
                entryRepository.updateLastAccess(configuration.getCacheName(), cacheKey.getKeyHash(), access, nextExpiry);
                return decodeValue(existing.get().getValuePayload(), argument);
            }
            return Optional.empty();
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        ArgumentUtils.requireNonNull("key", key);
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        if (value == null) {
            invalidate(key);
            return;
        }

        Instant now = Instant.now();

        CacheEntryEntity entity = new CacheEntryEntity();
        entity.setId(new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash()));
        entity.setKeyPayload(cacheKey.getKeyPayload());
        entity.setCreatedAt(now);
        entity.setLastAccessAt(now);
        entity.setExpiresAt(computeExpiry(now, now).orElse(null));
        entity.setValueWeight(1L);
        entity.setValuePayload(encodeValue(value));
        entryRepository.save(entity);
    }

    @Override
    public void invalidate(@NonNull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        entryRepository.invalidateKey(configuration.getCacheName(), cacheKey.getKeyHash());
    }

    @Override
    public void invalidateAll() {
        entryRepository.invalidateCache(configuration.getCacheName());
    }

    @Override
    public String getName() {
        return configuration.getCacheName();
    }

    @Override
    public OracleCacheEntryRepository getNativeCache() {
        return entryRepository;
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        return Publishers.just(new CacheInfo() {
            @NonNull
            @Override
            public String getName() {
                return configuration.getCacheName();
            }

            @NonNull
            @Override
            public Map<String, Object> get() {
                Map<String, Object> oracle = new LinkedHashMap<>(6);
                oracle.put("blocking", configuration.isBlocking());
                oracle.put("cleanupIntervalSeconds", cleanupIntervalSeconds());
                oracle.put("lockWaitTimeoutMs", configuration.getLockWaitTimeout().toMillis());
                oracle.put("entryCount", entryRepository.countByIdCacheName(configuration.getCacheName()));
                oracle.put("totalWeight", entryRepository.totalWeight(configuration.getCacheName()));

                Map<String, Object> result = new LinkedHashMap<>(3);
                result.put("implementationClass", OracleSyncCache.class.getName());
                result.put("nativeClass", entryRepository.getClass().getName());
                result.put("oracle", oracle);
                return result;
            }
        });
    }

    void runCleanup() {
        entryRepository.runCleanupProcedure(configuration.getCacheName(), CLEANUP_BATCH_SIZE);
    }

    private long cleanupIntervalSeconds() {
        return Math.max(1, configuration.getCleanupInterval().toSeconds());
    }

    private <T> Optional<T> getBySerializedKey(OracleCacheKey cacheKey, Argument<T> requiredType) {
        Optional<CacheEntryEntity> existing = entryRepository.findByIdCacheNameAndIdKeyHash(
            configuration.getCacheName(),
            cacheKey.getKeyHash()
        );
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        CacheEntryEntity entity = existing.get();
        if (isExpired(entity, now)) {
            entryRepository.invalidateKey(configuration.getCacheName(), cacheKey.getKeyHash());
            return Optional.empty();
        }
        if (!hasStoredValue(entity.getValuePayload())) {
            return Optional.empty();
        }

        Instant nextExpiry = computeExpiry(now, entity.getCreatedAt()).orElse(entity.getExpiresAt());
        entryRepository.updateLastAccess(configuration.getCacheName(), cacheKey.getKeyHash(), now, nextExpiry);
        return decodeValue(entity.getValuePayload(), requiredType);
    }

    private <T> T loadWithDatabaseCoordination(OracleCacheKey cacheKey,
                                               Argument<T> requiredType,
                                               Supplier<T> supplier) {
        T supplied = supplier.get();
        Instant now = Instant.now();
        Instant expiresAt = computeExpiry(now, now).orElse(null);
        String status;
        try {
            status = entryRepository.blockingPut(
                configuration.getCacheName(),
                cacheKey.getKeyHash(),
                cacheKey.getKeyPayload(),
                encodeValue(supplied),
                expiresAt,
                1L
            );
        } catch (RuntimeException e) {
            if (isDuplicateKeyViolation(e)) {
                Optional<T> existing = getBySerializedKey(cacheKey, requiredType);
                if (existing.isPresent()) {
                    return existing.get();
                }
                throw new IllegalStateException("Blocking cache put collided but no cached value was available", e);
            }
            throw e;
        }

        if ("SUCCESS".equals(status)) {
            return supplied;
        }

        if ("ALREADY_INSERTED".equals(status)) {
            Optional<T> existing = getBySerializedKey(cacheKey, requiredType);
            if (existing.isPresent()) {
                return existing.get();
            }
            throw new IllegalStateException("Blocking cache put reported ALREADY_INSERTED but no cached value was available");
        }

        throw new IllegalStateException("Blocking cache put returned unexpected status: " + status);
    }

    private Optional<Instant> computeExpiry(Instant now, @Nullable Instant createdAt) {
        Optional<Instant> writeExpiry = computeWriteExpiry(createdAt != null ? createdAt : now);
        Optional<Instant> accessExpiry = computeAccessExpiry(now);

        if (writeExpiry.isPresent() && accessExpiry.isPresent()) {
            Instant writeAt = writeExpiry.get();
            Instant accessAt = accessExpiry.get();
            return Optional.of(writeAt.isBefore(accessAt) ? writeAt : accessAt);
        }
        if (writeExpiry.isPresent()) {
            return writeExpiry;
        }
        if (accessExpiry.isPresent()) {
            return accessExpiry;
        }
        return Optional.empty();
    }

    private Optional<Instant> computeWriteExpiry(Instant now) {
        Optional<Duration> afterWrite = configuration.getExpireAfterWrite();
        if (afterWrite.isPresent()) {
            return Optional.of(now.plus(afterWrite.get()));
        }
        return Optional.empty();
    }

    private Optional<Instant> computeAccessExpiry(Instant now) {
        Optional<Duration> afterAccess = configuration.getExpireAfterAccess();
        if (afterAccess.isPresent()) {
            return Optional.of(now.plus(afterAccess.get()));
        }
        return Optional.empty();
    }

    private boolean isExpired(CacheEntryEntity entity, Instant now) {
        Instant expiresAt = entity.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(now);
    }

    private byte[] encodeValue(Object value) {
        try {
            return jsonMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode cache value as JSON", e);
        }
    }

    private <T> Optional<T> decodeValue(byte[] payload, Argument<T> requiredType) {
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jsonMapper.readValue(payload, requiredType));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private boolean hasStoredValue(@Nullable byte[] payload) {
        return payload != null && payload.length > 0;
    }

    private boolean isDuplicateKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 1) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("ORA-00001")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
