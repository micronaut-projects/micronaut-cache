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
package io.micronaut.cache.coherence;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.cache.DynamicCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.scheduling.TaskExecutors;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.cache.CacheManager} implementation for Coherence.
 *
 * @author Vaso Putica
 */
@Singleton
public class CoherenceCacheManager implements DynamicCacheManager<NamedCache<Object, Object>> {

    private final ConversionService<?> conversionService;
    private final ExecutorService executorService;
    private final Session session;

    public CoherenceCacheManager(ConversionService<?> conversionService,
                                 @Named(TaskExecutors.IO) ExecutorService executorService,
                                 Coherence coherence) {
        this.conversionService = conversionService;
        this.executorService = executorService;
        this.session = coherence.getSession();
    }

    @NonNull
    @Override
    public SyncCache<NamedCache<Object, Object>> getCache(String name) {
        NamedCache<Object, Object> nativeCache = session.getCache(name);
        return new CoherenceSyncCache(conversionService, nativeCache, executorService);
    }
}
