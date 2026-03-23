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
import io.micronaut.cache.oracle.persistence.OracleCacheStatsRepository;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Synchronous Oracle cache implementation.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
public final class OracleSyncCache implements SyncCache<OracleCacheEntryRepository> {

    private final OracleCacheConfiguration configuration;
    private final OracleCacheEntryRepository entryRepository;
    private final OracleCacheStatsRepository statsRepository;
    private final ExecutorService executorService;
    private final OracleKeySerializer keySerializer;
    private final JsonMapper jsonMapper;

    public OracleSyncCache(OracleCacheConfiguration configuration,
                           OracleCacheEntryRepository entryRepository,
                           OracleCacheStatsRepository statsRepository,
                           ExecutorService executorService,
                           OracleKeySerializer keySerializer,
                           JsonMapper jsonMapper) {
        this.configuration = configuration;
        this.entryRepository = entryRepository;
        this.statsRepository = statsRepository;
        this.executorService = executorService;
        this.keySerializer = keySerializer;
        this.jsonMapper = jsonMapper;
    }

    @NonNull
    @Override
    public <T> Optional<T> get(@NonNull Object key, @NonNull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("requiredType", requiredType);
        Optional<T> result = getBySerializedKey(keySerializer.serialize(key), requiredType);
        recordStats(result.isPresent() ? 1 : 0, result.isPresent() ? 0 : 1, 0, 0);
        return result;
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("requiredType", requiredType);
        ArgumentUtils.requireNonNull("supplier", supplier);

        OracleCacheKey cacheKey = keySerializer.serialize(key);
        Optional<T> existing = getBySerializedKey(cacheKey, requiredType);
        if (existing.isPresent()) {
            // Cache Hit
            recordStats(1, 0, 0, 0);
            return existing.get();
        }
        else {
            // Cache Miss
            recordStats(0, 1, 0, 0);
            T supplied = supplier.get();
            if (configuration.isBlocking()) {
                String status = blockingPutBySerializedKey(cacheKey, supplied, 0L);
                if (!isSuccessfulBlockingPutStatus(status)) {
                    throw new IllegalStateException("Blocking cache put returned unexpected status: " + status);
                }
            } else {
                putBySerializedKey(cacheKey, supplied);
            }
            // Cache Put
            recordStats(0, 0, 1, 0);
            return supplied;
        }
    }

    private <T> Optional<T> getBySerializedKey(OracleCacheKey cacheKey, Argument<T> requiredType) {
        Optional<CacheEntryEntity> existing = entryRepository.findById(
            new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash())
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
        if (!hasStoredValue(entity.valuePayload())) {
            return Optional.empty();
        }

        Instant nextExpiry = computeExpiry(now, entity.createdAt()).orElse(entity.expiresAt());
        entryRepository.updateLastAccess(configuration.getCacheName(), cacheKey.getKeyHash(), now, nextExpiry);
        return decodeValue(entity.valuePayload(), requiredType);
    }

    @NonNull
    @Override
    public <T> Optional<T> putIfAbsent(@NonNull Object key, @NonNull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);

        @SuppressWarnings("unchecked")
        Argument<T> argument = (Argument<T>) Argument.of(value.getClass());
        OracleCacheKey cacheKey = keySerializer.serialize(key);

        if (configuration.isBlocking()) {
            String status = blockingPutBySerializedKey(cacheKey, value, 1L);
            if ("INSERTED".equals(status)) {
                recordStats(0, 0, 1, 0);
                return Optional.empty();
            }
            if ("UPDATED_TIMESTAMP".equals(status)) {
                Optional<CacheEntryEntity> existing = entryRepository.findById(
                    new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash())
                );
                if (existing.isPresent() && !isExpired(existing.get(), Instant.now()) && hasStoredValue(existing.get().valuePayload())) {
                    recordStats(1, 0, 0, 0);
                    return decodeValue(existing.get().valuePayload(), argument);
                }
                return Optional.empty();
            }
            throw new IllegalStateException("Blocking putIfAbsent returned unexpected status: " + status);
        } else {
            CacheEntryEntity entity = newEntry(cacheKey, value);
            try {
                entryRepository.save(entity);
                // Cache Put
                recordStats(0, 0, 1, 0);
                return Optional.empty();
            } catch (RuntimeException e) {
                if (!isDuplicateKeyViolation(e)) {
                    throw e;
                }
                Optional<CacheEntryEntity> existing = entryRepository.findById(
                    new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash())
                );
                if (existing.isPresent() && !isExpired(existing.get(), Instant.now()) && hasStoredValue(existing.get().valuePayload())) {
                    Instant access = Instant.now();
                    Instant nextExpiry = computeExpiry(access, existing.get().createdAt()).orElse(existing.get().expiresAt());
                    entryRepository.updateLastAccess(configuration.getCacheName(), cacheKey.getKeyHash(), access, nextExpiry);
                    // Cache Hit
                    recordStats(1, 0, 0, 0);
                    return decodeValue(existing.get().valuePayload(), argument);
                }
                return Optional.empty();
            }
        }

        
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        ArgumentUtils.requireNonNull("key", key);
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        putBySerializedKey(cacheKey, value);
    }

    private void putBySerializedKey(OracleCacheKey cacheKey, @Nullable Object value) {
        if (value == null) {
            long invalidated = entryRepository.invalidateKey(configuration.getCacheName(), cacheKey.getKeyHash());
            recordStats(0, 0, 0, invalidated);
            return;
        }

        if (configuration.isBlocking()) {
            String status = blockingPutBySerializedKey(cacheKey, value, 0L);
            if (!isSuccessfulBlockingPutStatus(status)) {
                throw new IllegalStateException("Blocking cache put returned unexpected status: " + status);
            }
        } else {
            CacheEntryEntity entity = newEntry(cacheKey, value);
            try {
                entryRepository.save(entity);
            } catch (RuntimeException e) {
                if (!isDuplicateKeyViolation(e)) {
                    throw e;
                }
                entryRepository.invalidateKey(configuration.getCacheName(), cacheKey.getKeyHash());
                entryRepository.save(entity);
            }
        }
        recordStats(0, 0, 1, 0);
    }

    private CacheEntryEntity newEntry(OracleCacheKey cacheKey, Object value) {
        byte[] valuePayload = encodeValue(value);
        Instant now = Instant.now();
        return new CacheEntryEntity(
            new CacheEntryId(configuration.getCacheName(), cacheKey.getKeyHash()),
            cacheKey.getKeyPayload(),
            valuePayload,
            (long) valuePayload.length,
            now,
            now,
            computeExpiry(now, now).orElse(null)
        );
    }

    private String blockingPutBySerializedKey(OracleCacheKey cacheKey, Object suppliedValue, long insertOnly) {
        CacheEntryEntity entry = newEntry(cacheKey, suppliedValue);
        return entryRepository.blockingPut(
            configuration.getCacheName(),
            cacheKey.getKeyHash(),
            cacheKey.getKeyPayload(),
            entry.valuePayload(),
            entry.expiresAt(),
            entry.valueWeight(),
            insertOnly
        );
    }

    @Override
    public void invalidate(@NonNull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        long invalidated = entryRepository.invalidateKey(configuration.getCacheName(), cacheKey.getKeyHash());
        recordStats(0, 0, 0, invalidated);
    }

    @Override
    public void invalidateAll() {
        long invalidated = entryRepository.invalidateCache(configuration.getCacheName());
        recordStats(0, 0, 0, invalidated);
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
    public ExecutorService getExecutorService() {
        return executorService;
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

    public void runCleanup() {
        entryRepository.runCleanupProcedure(configuration.getCacheName(), configuration.getCleanupBatchSize());
    }

    private long cleanupIntervalSeconds() {
        return Math.max(1, configuration.getCleanupInterval().toSeconds());
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
        Instant expiresAt = entity.expiresAt();
        return expiresAt != null && expiresAt.isBefore(now);
    }

    private boolean isSuccessfulBlockingPutStatus(String status) {
        return "INSERTED".equals(status)
            || "UPDATED_PAYLOAD".equals(status)
            || "UPDATED".equals(status);
    }

    private void recordStats(long hitDelta, long missDelta, long putDelta, long invalidateDelta) {
        if (!configuration.isRecordStats()) {
            return;
        }
        statsExecutor().execute(() -> {
            try {
                statsRepository.updateStats(
                    configuration.getCacheName(),
                    hitDelta,
                    missDelta,
                    putDelta,
                    invalidateDelta,
                    Instant.now()
                );
            } catch (RuntimeException ignored) {
            }
        });
    }

    private Executor statsExecutor() {
        ExecutorService executorService = getExecutorService();
        if (executorService != null) {
            return executorService;
        }
        return Runnable::run;
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
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to decode cache value as JSON for type " + requiredType.getType().getName(),
                e
            );
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
