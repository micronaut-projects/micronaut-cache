/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.infinispan.client.hotrod.RemoteCacheManager;

import javax.inject.Singleton;

/**
 * Factory class that creates an Infinispan {@link RemoteCacheManager}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see InfinispanHotRodClientConfiguration#getBuilder()
 */
@Factory
public class InfinispanCacheFactory {

    private InfinispanHotRodClientConfiguration configuration;

    /**
     * @param configuration the Infinispan client configuration
     */
    public InfinispanCacheFactory(InfinispanHotRodClientConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return a {@link RemoteCacheManager} instance.
     */
    @Singleton
    @Bean(preDestroy = "close")
    RemoteCacheManager remoteCacheManager() {
        return new RemoteCacheManager(configuration.getBuilder().build());
    }

}
