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
import io.micronaut.serde.oracle.jdbc.json.OracleJdbcJsonBinaryObjectMapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Synchronous Oracle cache implementation.
 *
 * @author Davide Cocco
 * @since 6.1.0
 */
public final class OracleSyncCache implements SyncCache<OracleCacheEntryRepository> {
    private static final Logger LOG = LoggerFactory.getLogger(OracleSyncCache.class);

    private final OracleCacheConfiguration configuration;
    private final OracleCacheEntryRepository entryRepository;
    private final OracleCacheStatsRepository statsRepository;
    private final ExecutorService executorService;
    private final OracleKeySerializer keySerializer;
    private final OracleJdbcJsonBinaryObjectMapper jsonMapper;
    private final AtomicLong unavailableUntilNanos = new AtomicLong();

    public OracleSyncCache(OracleCacheConfiguration configuration,
                           OracleCacheEntryRepository entryRepository,
                           OracleCacheStatsRepository statsRepository,
                           ExecutorService executorService,
                           OracleKeySerializer keySerializer,
                           OracleJdbcJsonBinaryObjectMapper jsonMapper) {
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
        CacheLookupResult<T> result = getBySerializedKey(keySerializer.serialize(key), requiredType);
        if (result.status() == CacheLookupStatus.HIT) {
            recordStats(1, 0, 0, 0);
        } else if (result.status() == CacheLookupStatus.MISS) {
            recordStats(0, 1, 0, 0);
        }
        return result.value();
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Argument<T> requiredType, @NonNull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("requiredType", requiredType);
        ArgumentUtils.requireNonNull("supplier", supplier);

        OracleCacheKey cacheKey = keySerializer.serialize(key);
        CacheLookupResult<T> existing = getBySerializedKey(cacheKey, requiredType);
        if (existing.status() == CacheLookupStatus.HIT) {
            // Cache Hit
            recordStats(1, 0, 0, 0);
            return existing.value().get();
        }
        if (existing.status() == CacheLookupStatus.UNAVAILABLE) {
            return supplier.get();
        }

        // Cache Miss
        recordStats(0, 1, 0, 0);
        T supplied = supplier.get();
        CacheWriteResult writeResult = putBySerializedKey(cacheKey, supplied);
        if (writeResult.status() == CacheWriteStatus.WRITTEN) {
            // Cache Put
            recordStats(0, 0, 1, 0);
        }
        return supplied;
    }

    private <T> CacheLookupResult<T> getBySerializedKey(OracleCacheKey cacheKey, Argument<T> requiredType) {
        if (shouldBypassCache()) {
            return CacheLookupResult.unavailable();
        }
        CacheEntryId cacheEntryId = new CacheEntryId(configuration.getCacheName(), cacheKey.keyHash());
        Optional<CacheEntryEntity> existing;
        try {
            existing = entryRepository.findById(cacheEntryId);
        } catch (RuntimeException e) {
            return unavailableLookup("find cache entry", e);
        }
        if (existing.isEmpty()) {
            return CacheLookupResult.miss();
        }

        Instant now = Instant.now();
        CacheEntryEntity entity = existing.get();
        if (isExpired(entity, now)) {
            try {
                entryRepository.delete(cacheEntryId);
            } catch (RuntimeException e) {
                handleRepositoryFailure("delete expired cache entry", e);
            }
            return CacheLookupResult.miss();
        }
        if (!hasStoredValue(entity.valuePayload())) {
            return CacheLookupResult.miss();
        }

        Instant nextExpiry = computeExpiry(now, entity.createdAt()).orElse(entity.expiresAt());
        try {
            entryRepository.update(cacheEntryId, now, nextExpiry);
        } catch (RuntimeException e) {
            handleRepositoryFailure("refresh cache entry access", e);
        }
        Optional<T> decoded = decodeValue(entity.valuePayload(), requiredType);
        if (decoded.isPresent()) {
            return CacheLookupResult.hit(decoded.get());
        }
        return CacheLookupResult.miss();
    }

    @NonNull
    @Override
    public <T> Optional<T> putIfAbsent(@NonNull Object key, @NonNull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);

        @SuppressWarnings("unchecked")
        Argument<T> argument = (Argument<T>) Argument.of(value.getClass());
        if (shouldBypassCache()) {
            return Optional.empty();
        }
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        CacheEntryEntity entity = newEntry(cacheKey, value);

        if (configuration.isBlocking()) {
            String status;
            try {
                status = blockingPut(cacheKey, entity, 1L);
            } catch (RuntimeException e) {
                handleRepositoryFailure("write cache entry through blocking procedure", e);
                return Optional.empty();
            }
            if ("INSERTED".equals(status)) {
                recordStats(0, 0, 1, 0);
                return Optional.empty();
            }
            if ("UPDATED_TIMESTAMP".equals(status)) {
                CacheLookupResult<T> existing = getBySerializedKey(cacheKey, argument);
                if (existing.status() == CacheLookupStatus.HIT) {
                    recordStats(1, 0, 0, 0);
                    return existing.value();
                }
                return Optional.empty();
            }
            throw new IllegalStateException("Blocking putIfAbsent returned unexpected status: " + status);
        } else {
            try {
                entryRepository.save(entity);
                // Cache Put
                recordStats(0, 0, 1, 0);
                return Optional.empty();
            } catch (RuntimeException e) {
                if (!isDuplicateKeyViolation(e)) {
                    handleRepositoryFailure("save cache entry", e);
                    return Optional.empty();
                }
                CacheLookupResult<T> existing = getBySerializedKey(cacheKey, argument);
                if (existing.status() == CacheLookupStatus.HIT) {
                    // Cache Hit
                    recordStats(1, 0, 0, 0);
                    return existing.value();
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        ArgumentUtils.requireNonNull("key", key);
        if (shouldBypassCache()) {
            return;
        }
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        CacheWriteResult writeResult = putBySerializedKey(cacheKey, value);
        if (writeResult.status() == CacheWriteStatus.WRITTEN) {
            recordStats(0, 0, 1, 0);
        } else if (writeResult.status() == CacheWriteStatus.REMOVED) {
            recordStats(0, 0, 0, writeResult.invalidated());
        }
    }

    private CacheWriteResult putBySerializedKey(OracleCacheKey cacheKey, @Nullable Object value) {
        if (shouldBypassCache()) {
            return CacheWriteResult.unavailable();
        }
        CacheEntryId cacheEntryId = new CacheEntryId(configuration.getCacheName(), cacheKey.keyHash());
        if (value == null) {
            try {
                return CacheWriteResult.removed(entryRepository.delete(cacheEntryId));
            } catch (RuntimeException e) {
                handleRepositoryFailure("delete cache entry", e);
                return CacheWriteResult.unavailable();
            }
        }

        CacheEntryEntity entity = newEntry(cacheKey, value);
        if (configuration.isBlocking()) {
            String status;
            try {
                status = blockingPut(cacheKey, entity, 0L);
            } catch (RuntimeException e) {
                handleRepositoryFailure("write cache entry through blocking procedure", e);
                return CacheWriteResult.unavailable();
            }
            if (!isSuccessfulBlockingPutStatus(status)) {
                throw new IllegalStateException("Blocking cache put returned unexpected status: " + status);
            }
        } else {
            try {
                entryRepository.save(entity);
            } catch (RuntimeException e) {
                if (!isDuplicateKeyViolation(e)) {
                    handleRepositoryFailure("save cache entry", e);
                    return CacheWriteResult.unavailable();
                }
                try {
                    entryRepository.delete(cacheEntryId);
                    entryRepository.save(entity);
                } catch (RuntimeException replacementFailure) {
                    handleRepositoryFailure("replace cache entry", replacementFailure);
                    return CacheWriteResult.unavailable();
                }
            }
        }
        return CacheWriteResult.written();
    }

    private CacheEntryEntity newEntry(OracleCacheKey cacheKey, Object value) {
        byte[] valuePayload = encodeValue(value);
        Instant now = Instant.now();
        return new CacheEntryEntity(
            new CacheEntryId(configuration.getCacheName(), cacheKey.keyHash()),
            cacheKey.keyPayload(),
            valuePayload,
            (long) valuePayload.length,
            now,
            now,
            computeExpiry(now, now).orElse(null)
        );
    }

    private String blockingPut(OracleCacheKey cacheKey, CacheEntryEntity entry, long insertOnly) {
        return entryRepository.blockingPut(
            configuration.getCacheName(),
            cacheKey.keyHash(),
            cacheKey.keyPayload(),
            entry.valuePayload(),
            entry.expiresAt(),
            entry.valueWeight(),
            insertOnly
        );
    }

    @Override
    public void invalidate(@NonNull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        if (shouldBypassCache()) {
            return;
        }
        OracleCacheKey cacheKey = keySerializer.serialize(key);
        CacheEntryId cacheEntryId = new CacheEntryId(configuration.getCacheName(), cacheKey.keyHash());
        long invalidated;
        try {
            invalidated = entryRepository.delete(cacheEntryId);
        } catch (RuntimeException e) {
            handleRepositoryFailure("invalidate cache entry", e);
            return;
        }
        recordStats(0, 0, 0, invalidated);
    }

    @Override
    public void invalidateAll() {
        if (shouldBypassCache()) {
            return;
        }
        long invalidated;
        try {
            invalidated = entryRepository.deleteByCacheName(configuration.getCacheName());
        } catch (RuntimeException e) {
            handleRepositoryFailure("invalidate cache entries", e);
            return;
        }
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
        return Publishers.just(new OracleCacheInfo());
    }

    public void runCleanup() {
        if (shouldBypassCache()) {
            return;
        }
        try {
            entryRepository.runCleanupProcedure(configuration.getCacheName(), configuration.getCleanupBatchSize());
        } catch (RuntimeException e) {
            handleRepositoryFailure("run cache cleanup", e);
        }
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

    private boolean shouldBypassCache() {
        return configuration.isFailOpen() && System.nanoTime() < unavailableUntilNanos.get();
    }

    private <T> CacheLookupResult<T> unavailableLookup(String operation, RuntimeException e) {
        handleRepositoryFailure(operation, e);
        return CacheLookupResult.unavailable();
    }

    private void handleRepositoryFailure(String operation, RuntimeException e) {
        if (!configuration.isFailOpen()) {
            throw e;
        }
        markCacheUnavailable(operation, e);
    }

    private void markCacheUnavailable(String operation, RuntimeException e) {
        Duration retryInterval = configuration.getFailOpenRetryInterval();
        long unavailableUntil = System.nanoTime() + retryInterval.toNanos();
        unavailableUntilNanos.accumulateAndGet(unavailableUntil, Math::max);
        LOG.debug(
            "Oracle cache {} failed during {}; bypassing repository calls for {}",
            configuration.getCacheName(),
            operation,
            retryInterval,
            e
        );
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
            } catch (RuntimeException e) {
                LOG.debug("Failed to update Oracle cache stats for cache {}", configuration.getCacheName(), e);
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
        } catch (Exception e) {
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

    private enum CacheLookupStatus {
        HIT,
        MISS,
        UNAVAILABLE
    }

    private record CacheLookupResult<T>(CacheLookupStatus status, Optional<T> value) {

        static <T> CacheLookupResult<T> hit(T value) {
            return new CacheLookupResult<>(CacheLookupStatus.HIT, Optional.of(value));
        }

        static <T> CacheLookupResult<T> miss() {
            return new CacheLookupResult<>(CacheLookupStatus.MISS, Optional.empty());
        }

        static <T> CacheLookupResult<T> unavailable() {
            return new CacheLookupResult<>(CacheLookupStatus.UNAVAILABLE, Optional.empty());
        }
    }

    private enum CacheWriteStatus {
        WRITTEN,
        REMOVED,
        UNAVAILABLE
    }

    private record CacheWriteResult(CacheWriteStatus status, long invalidated) {

        static CacheWriteResult written() {
            return new CacheWriteResult(CacheWriteStatus.WRITTEN, 0);
        }

        static CacheWriteResult removed(long invalidated) {
            return new CacheWriteResult(CacheWriteStatus.REMOVED, invalidated);
        }

        static CacheWriteResult unavailable() {
            return new CacheWriteResult(CacheWriteStatus.UNAVAILABLE, 0);
        }
    }

    private final class OracleCacheInfo implements CacheInfo {

        @Override
        public @NonNull String getName() {
            return configuration.getCacheName();
        }

        @Override
        public @NonNull Map<String, Object> get() {
            Long entryCount = null;
            Long totalWeight = null;
            boolean temporarilyUnavailable = shouldBypassCache();
            if (!temporarilyUnavailable) {
                try {
                    entryCount = entryRepository.countByCacheName(configuration.getCacheName());
                    totalWeight = entryRepository.totalWeight(configuration.getCacheName());
                } catch (RuntimeException e) {
                    handleRepositoryFailure("read cache info", e);
                    temporarilyUnavailable = true;
                }
            }

            Map<String, Object> oracle = new LinkedHashMap<>(8);
            oracle.put("blocking", configuration.isBlocking());
            oracle.put("failOpen", configuration.isFailOpen());
            oracle.put("failOpenRetryIntervalMs", configuration.getFailOpenRetryInterval().toMillis());
            oracle.put("temporarilyUnavailable", temporarilyUnavailable);
            oracle.put("cleanupIntervalSeconds", cleanupIntervalSeconds());
            oracle.put("lockWaitTimeoutMs", configuration.getLockWaitTimeout().toMillis());
            oracle.put("entryCount", entryCount);
            oracle.put("totalWeight", totalWeight);

            Map<String, Object> result = new LinkedHashMap<>(3);
            result.put("implementationClass", OracleSyncCache.class.getName());
            result.put("nativeClass", entryRepository.getClass().getName());
            result.put("oracle", oracle);
            return result;
        }
    }

}
