/*
 * Copyright 2017-2021 original authors
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

import com.tangosol.net.NamedCache;
import io.micronaut.cache.AbstractMapBasedSyncCache;
import io.micronaut.cache.AsyncCache;
import io.micronaut.core.convert.ConversionService;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.concurrent.ExecutorService;

/**
 * A {@link AsyncCache} implementation based on Coherence.
 *
 * @author Vaso Putica
 */
public class CoherenceSyncCache extends AbstractMapBasedSyncCache<NamedCache<Object, Object>> {

    private final ExecutorService executorService;

    /**
     * @param conversionService the conversion service
     * @param nativeCache the native cache
     * @param executorService managers the pool of executors
     */
    public CoherenceSyncCache(ConversionService<?> conversionService,
                              NamedCache<Object, Object> nativeCache,
                              ExecutorService executorService) {
        super(conversionService, nativeCache);
        this.executorService = executorService;
    }

    @Override
    public String getName() {
        return getNativeCache().getName();
    }

    @NonNull
    @Override
    public AsyncCache<NamedCache<Object, Object>> async() {
        return new CoherenceAsyncCache(getConversionService(), getNativeCache(), executorService);
    }
}
