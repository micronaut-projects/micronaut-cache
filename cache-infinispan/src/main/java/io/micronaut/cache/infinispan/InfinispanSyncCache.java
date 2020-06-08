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
package io.micronaut.cache.infinispan;

import io.micronaut.cache.AbstractMapBasedSyncCache;
import io.micronaut.cache.AsyncCache;
import io.micronaut.cache.CacheInfo;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import org.infinispan.client.hotrod.RemoteCache;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

/**
 * A {@link io.micronaut.cache.SyncCache} implementation based on Infinispan's {@link RemoteCache}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class InfinispanSyncCache extends AbstractMapBasedSyncCache<RemoteCache<Object, Object>> {

    /**
     * @param conversionService the conversion service
     * @param nativeCache       the native cache
     */
    public InfinispanSyncCache(ConversionService<?> conversionService, RemoteCache<Object, Object> nativeCache) {
        super(conversionService, nativeCache);
    }

    @Override
    public String getName() {
        return getNativeCache().getName();
    }

    @Nonnull
    @Override
    public AsyncCache<RemoteCache<Object, Object>> async() {
        return new InfinispanAsyncCache(getNativeCache(), getConversionService());
    }

    @Override
    public Publisher<CacheInfo> getCacheInfo() {
        return Publishers.just(new InfinispanCacheInfo(getNativeCache()));
    }

}
