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
package io.micronaut.cache.ignite;

import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration;
import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.TaskExecutors;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Factory class that creates a {@link org.apache.ignite.client.IgniteClient}, an {@link IgniteSyncCache}.
 *
 * @author Michael Pollind
 */
@Factory
public class IgniteCacheFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IgniteCacheFactory.class);
    private final List<IgniteClient> sessions = new ArrayList<>(2);

    /**
     * @param configuration The client configuration
     * @return The {@link Ignite}
     */
    @EachBean(IgniteClientConfiguration.class)
    @Bean(preDestroy = "close")
    public IgniteClient igniteClient(IgniteClientConfiguration configuration) {
        IgniteClient ignite = Ignition.startClient(configuration.getClient());
        sessions.add(ignite);
        return ignite;
    }


    /**
     * @param configuration   the configuration
     * @param service         the conversion service
     * @return the sync cache
     * @throws Exception when client can't be found for cache
     */
    @EachBean(IgniteCacheConfiguration.class)
    @Requires(beans = IgniteClient.class)
    public IgniteSyncCache syncCacheThin(
        IgniteCacheConfiguration configuration,
        ConversionService<?> service,
        @Named(TaskExecutors.IO) ExecutorService executorService,
        BeanContext beanContext) throws Exception {
        Optional<IgniteClient> client = beanContext.findBean(IgniteClient.class, Qualifiers.byName(configuration.getClient()));
        if(client.isPresent()){
            return new IgniteSyncCache(service,executorService, client.get().getOrCreateCache(configuration.getConfiguration()));
        }
        throw new Exception("");
    }

    @Override
    public void close() throws Exception {
        for (IgniteClient sess : sessions) {
            try {
                sess.close();
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error closing ignite [" + sess + "]: " + e.getMessage(), e);
                }
            }
        }
    }
}
