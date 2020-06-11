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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.scheduling.TaskExecutors;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Factory class that creates a {@link org.apache.ignite.client.IgniteClient}, an {@link IgniteSyncCache}.
 *
 * @author Michael Pollind
 */
@Factory
public class IgniteCacheFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IgniteCacheFactory.class);
    private List<Ignite> sessions = new ArrayList<>(2);

    /**
     * @param configuration The client configuration
     * @return The {@link Ignite}
     */
    @EachBean(IgniteClientConfiguration.class)
    @Bean(preDestroy = "close")
    public Ignite igniteClient(IgniteClientConfiguration configuration) {
        Ignite ignite = Ignition.start(configuration.build());
        sessions.add(ignite);
        return ignite;
    }

    /**
     * @param configuration   the configuration
     * @param service         the conversion service
     * @param clients         list of {@link Ignite} clients
     * @param executorService the executor
     * @return the sync cache
     * @throws Exception when client can't be found for cache
     */
    @EachBean(IgniteCacheConfiguration.class)
    public IgniteSyncCache syncCache(
        IgniteCacheConfiguration configuration,
        ConversionService<?> service,
        List<Ignite> clients,
        @Named(TaskExecutors.IO) ExecutorService executorService) throws Exception {
        for (Ignite client : clients) {
            if (client.name().equals(configuration.getClient())) {
                return new IgniteSyncCache(service, client.getOrCreateCache(configuration.build()), executorService);
            }
        }
        throw new Exception("Can't find ignite client for: " + configuration.getClient());
    }

    @Override
    public void close() throws Exception {
        for (Ignite sess : sessions) {
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
