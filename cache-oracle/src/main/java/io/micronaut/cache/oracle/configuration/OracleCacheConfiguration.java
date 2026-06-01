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
package io.micronaut.cache.oracle.configuration;

import io.micronaut.cache.CacheConfiguration;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.ApplicationConfiguration;

import java.time.Duration;

/**
 * Per-cache Oracle provider configuration under {@code micronaut.caches.*}.
 *
 * @author Davide Cocco
 * @since 6.0.0
 */
@EachProperty(CacheConfiguration.PREFIX)
public final class OracleCacheConfiguration extends CacheConfiguration {

    /**
     * Default lock wait timeout.
     */
    public static final Duration DEFAULT_LOCK_WAIT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Default cleanup interval.
     */
    public static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(60);

    /**
     * Default cleanup batch size.
     */
    public static final long DEFAULT_CLEANUP_BATCH_SIZE = 100L;

    private boolean blocking;
    private Duration lockWaitTimeout = DEFAULT_LOCK_WAIT_TIMEOUT;
    private Duration cleanupInterval = DEFAULT_CLEANUP_INTERVAL;
    private long cleanupBatchSize = DEFAULT_CLEANUP_BATCH_SIZE;

    /**
     * Creates a new Oracle cache configuration.
     *
     * @param cacheName The cache name
     * @param applicationConfiguration The application configuration
     */
    public OracleCacheConfiguration(@Parameter String cacheName, ApplicationConfiguration applicationConfiguration) {
        super(cacheName, applicationConfiguration);
    }

    /**
     * Checks whether blocking writes are enabled.
     *
     * @return Whether writes use the database-coordinated blocking procedure
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Sets whether blocking writes are enabled.
     *
     * @param blocking Whether writes should use the database-coordinated blocking procedure
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    /**
     * Gets the lock wait timeout.
     *
     * @return The lock wait timeout used by blocking cache writes
     */
    @NonNull
    public Duration getLockWaitTimeout() {
        return lockWaitTimeout;
    }

    /**
     * Sets the lock wait timeout.
     *
     * @param lockWaitTimeout The lock wait timeout used by blocking cache writes
     */
    public void setLockWaitTimeout(@NonNull Duration lockWaitTimeout) {
        if (lockWaitTimeout.isNegative()) {
            throw new IllegalArgumentException("lockWaitTimeout cannot be negative");
        }
        this.lockWaitTimeout = lockWaitTimeout;
    }

    /**
     * Gets the cleanup interval.
     *
     * @return The cleanup interval
     */
    @NonNull
    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    /**
     * Sets the cleanup interval.
     *
     * @param cleanupInterval The cleanup interval
     */
    public void setCleanupInterval(@NonNull Duration cleanupInterval) {
        if (cleanupInterval.isNegative()) {
            throw new IllegalArgumentException("cleanupInterval cannot be negative");
        }
        this.cleanupInterval = cleanupInterval;
    }

    /**
     * Gets the cleanup batch size.
     *
     * @return The cleanup batch size
     */
    public long getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    /**
     * Sets the cleanup batch size.
     *
     * @param cleanupBatchSize The cleanup batch size
     */
    public void setCleanupBatchSize(long cleanupBatchSize) {
        if (cleanupBatchSize <= 0) {
            throw new IllegalArgumentException("cleanupBatchSize must be greater than 0");
        }
        this.cleanupBatchSize = cleanupBatchSize;
    }

    /**
     * @param maximumSize The maximum cache size
     */
    @Override
    public void setMaximumSize(Long maximumSize) {
        if (maximumSize != null && maximumSize < 0) {
            throw new IllegalArgumentException("maximumSize cannot be negative");
        }
        super.setMaximumSize(maximumSize);
    }

    /**
     * @param maximumWeight The maximum cache weight
     */
    @Override
    public void setMaximumWeight(Long maximumWeight) {
        if (maximumWeight != null && maximumWeight < 0) {
            throw new IllegalArgumentException("maximumWeight cannot be negative");
        }
        super.setMaximumWeight(maximumWeight);
    }

    /**
     * @param expireAfterWrite The expire-after-write duration
     */
    @Override
    public void setExpireAfterWrite(Duration expireAfterWrite) {
        if (expireAfterWrite != null && expireAfterWrite.isNegative()) {
            throw new IllegalArgumentException("expireAfterWrite cannot be negative");
        }
        super.setExpireAfterWrite(expireAfterWrite);
    }

    /**
     * @param expireAfterAccess The expire-after-access duration
     */
    @Override
    public void setExpireAfterAccess(Duration expireAfterAccess) {
        if (expireAfterAccess != null && expireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("expireAfterAccess cannot be negative");
        }
        super.setExpireAfterAccess(expireAfterAccess);
    }

}
