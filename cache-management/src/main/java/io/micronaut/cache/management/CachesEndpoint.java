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
package io.micronaut.cache.management;

import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.Cache;
import io.micronaut.cache.CacheInfo;
import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.management.endpoint.annotation.Delete;
import io.micronaut.management.endpoint.annotation.Endpoint;
import io.micronaut.management.endpoint.annotation.Read;
import io.micronaut.management.endpoint.annotation.Selector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exposes an {@link Endpoint} to manage caches.
 *
 * @author Marcel Overdijk
 * @author graemerocher
 * @since 1.1.0
 */
@Endpoint(id = CachesEndpoint.NAME, defaultEnabled = false)
public class CachesEndpoint {

    /**
     * Endpoint name.
     */
    public static final String NAME = "caches";

    private final CacheManager<Object> cacheManager;

    /**
     * @param cacheManager       The {@link CacheManager}
     */
    public CachesEndpoint(CacheManager<Object> cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Returns the caches as a {@link Mono}.
     *
     * @return The caches as a {@link Mono}
     */
    @Read
    public Mono<Map<String, Object>> getCaches() {
        return Flux.fromIterable(cacheManager.getCacheNames())
               .map(cacheManager::getCache)
               .map(SyncCache::getCacheInfo)
               .map(Flux::from)
               .flatMap(cacheInfoFlux -> cacheInfoFlux.take(1))
               .reduce(new HashMap<>(), (seed, info) -> {
                   seed.put(info.getName(), info.get());
                   return seed;
               })
               .map(objectObjectHashMap -> Collections.singletonMap(NAME, objectObjectHashMap));
    }

    /**
     * Returns the cache as a {@link Mono}.
     *
     * @param name The name of the cache to retrieve
     * @return The cache as a {@link Mono}
     */
    @Read
    public Mono<Map<String, Object>> getCache(@NotBlank @Selector String name) {
        return Mono.just(name)
                .map(cacheManager::getCache)
                .flux()
                .flatMap(Cache::getCacheInfo)
                .map(CacheInfo::get)
                .elementAt(0)
                .onErrorResume((Throwable e) -> Mono.empty());
    }

    /**
     * Invalidates all the caches.
     *
     * @return A maybe that emits a boolean.
     */
    @Delete
    public Mono<Boolean> invalidateCaches() {
        return Flux.fromIterable(cacheManager.getCacheNames())
               .map(cacheManager::getCache)
               .map(SyncCache::async)
               .map(AsyncCache::invalidateAll)
               .flatMap(Publishers::fromCompletableFuture)
               .reduce((aBoolean, aBoolean2) -> aBoolean && aBoolean2)
               .onErrorReturn(false);
    }

    /**
     * Invalidates the cache.
     *
     * @param name The name of the cache to invalidate
     * @return A maybe that emits a boolean if the operation was successful
     */
    @Delete
    public Mono<Boolean> invalidateCache(@NotBlank @Selector String name) {
        return Mono.just(name)
                .map(cacheManager::getCache)
                .map(SyncCache::async)
                .map(AsyncCache::invalidateAll)
                .flatMap(Mono::fromFuture)
                .onErrorReturn(false);
    }

    /**
     * Invalidates a key within the provided cache.
     *
     * @param name The name of the cache
     * @param key the key within the cache to invalidate
     * @return A maybe that emits a boolean if the operation was successful
     */
    @Delete
    public Mono<Boolean> invalidateCacheKey(@NotBlank @Selector String name, @NotBlank @Selector String key) {
        return Mono.just(name)
                .map(cacheManager::getCache)
                .map(SyncCache::async)
                .map(asyncCache -> asyncCache.invalidate(key))
                .flatMap(Mono::fromFuture)
                .onErrorReturn(false);
    }

}
