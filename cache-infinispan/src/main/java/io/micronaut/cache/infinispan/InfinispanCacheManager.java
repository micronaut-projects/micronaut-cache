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

import io.micronaut.cache.DynamicCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.BasicConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.inject.Singleton;

/**
 * A {@link DynamicCacheManager} that creates Infinispan caches on demand.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see org.infinispan.client.hotrod.RemoteCacheManagerAdmin#getOrCreateCache(String, BasicConfiguration)
 */
@Singleton
public class InfinispanCacheManager implements DynamicCacheManager<RemoteCache<Object, Object>> {

    private final RemoteCacheManager remoteCacheManager;
    private final ConversionService<?> conversionService;

    /**
     * @param remoteCacheManager the Infinispan remote cache manager
     * @param conversionService the conversion service
     */
    public InfinispanCacheManager(RemoteCacheManager remoteCacheManager, ConversionService<?> conversionService) {
        this.remoteCacheManager = remoteCacheManager;
        this.conversionService = conversionService;
    }

    @NonNull
    @Override
    public SyncCache<RemoteCache<Object, Object>> getCache(String name) {
        BasicConfiguration basicConfiguration = new ConfigurationBuilder().build();
        RemoteCache<Object, Object> nativeCache = remoteCacheManager.administration().getOrCreateCache(name, basicConfiguration);
        return new InfinispanSyncCache(conversionService, nativeCache);
    }
}
