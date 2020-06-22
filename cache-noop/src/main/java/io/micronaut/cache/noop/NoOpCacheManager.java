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
package io.micronaut.cache.noop;

import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A no operation {@link io.micronaut.cache.CacheManager} implementation suitable for disabling caching.
 *
 * <p>Will simply accept any items into the cache without actually storing them.
 *
 * @author Marcel Overdijk
 * @since 1.0.0
 */
@Replaces(DefaultCacheManager.class)
@Requires(property = "noop-cache.enabled", value = StringUtils.TRUE)
@Primary
public class NoOpCacheManager implements io.micronaut.cache.CacheManager<Object> {

    private Map<String, NoOpSyncCache> cacheMap;

    /**
     * Constructor.
     */
    public NoOpCacheManager() {
        this.cacheMap = new ConcurrentHashMap<>();
    }

    @Nonnull
    @Override
    public Set<String> getCacheNames() {
        return cacheMap.keySet();
    }

    @Nonnull
    @Override
    public SyncCache<Object> getCache(String name) {
        NoOpSyncCache syncCache = this.cacheMap.get(name);
        if (syncCache == null) {
            syncCache = new NoOpSyncCache(name);
            this.cacheMap.put(name, syncCache);
        }
        return syncCache;
    }
}
