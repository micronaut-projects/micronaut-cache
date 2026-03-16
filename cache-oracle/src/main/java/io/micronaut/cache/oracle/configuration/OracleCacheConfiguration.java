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
 * @since 5.0.0
 */
@EachProperty(CacheConfiguration.PREFIX)
public final class OracleCacheConfiguration extends CacheConfiguration {

    public static final Duration DEFAULT_LOCK_WAIT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(60);
    public static final long DEFAULT_CLEANUP_BATCH_SIZE = 100L;

    private boolean blocking;
    private Duration lockWaitTimeout = DEFAULT_LOCK_WAIT_TIMEOUT;
    private Duration cleanupInterval = DEFAULT_CLEANUP_INTERVAL;
    private long cleanupBatchSize = DEFAULT_CLEANUP_BATCH_SIZE;

    public OracleCacheConfiguration(@Parameter String cacheName, ApplicationConfiguration applicationConfiguration) {
        super(cacheName, applicationConfiguration);
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    @NonNull
    public Duration getLockWaitTimeout() {
        return lockWaitTimeout;
    }

    public void setLockWaitTimeout(@NonNull Duration lockWaitTimeout) {
        if (lockWaitTimeout.isNegative()) {
            throw new IllegalArgumentException("lockWaitTimeout cannot be negative");
        }
        this.lockWaitTimeout = lockWaitTimeout;
    }

    @NonNull
    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(@NonNull Duration cleanupInterval) {
        if (cleanupInterval.isNegative()) {
            throw new IllegalArgumentException("cleanupInterval cannot be negative");
        }
        this.cleanupInterval = cleanupInterval;
    }

    public long getCleanupBatchSize() {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(long cleanupBatchSize) {
        if (cleanupBatchSize <= 0) {
            throw new IllegalArgumentException("cleanupBatchSize must be greater than 0");
        }
        this.cleanupBatchSize = cleanupBatchSize;
    }

    @Override
    public void setMaximumSize(Long maximumSize) {
        if (maximumSize != null && maximumSize < 0) {
            throw new IllegalArgumentException("maximumSize cannot be negative");
        }
        super.setMaximumSize(maximumSize);
    }

    @Override
    public void setMaximumWeight(Long maximumWeight) {
        if (maximumWeight != null && maximumWeight < 0) {
            throw new IllegalArgumentException("maximumWeight cannot be negative");
        }
        super.setMaximumWeight(maximumWeight);
    }

    @Override
    public void setExpireAfterWrite(Duration expireAfterWrite) {
        if (expireAfterWrite != null && expireAfterWrite.isNegative()) {
            throw new IllegalArgumentException("expireAfterWrite cannot be negative");
        }
        super.setExpireAfterWrite(expireAfterWrite);
    }

    @Override
    public void setExpireAfterAccess(Duration expireAfterAccess) {
        if (expireAfterAccess != null && expireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("expireAfterAccess cannot be negative");
        }
        super.setExpireAfterAccess(expireAfterAccess);
    }

}
