package io.micronaut.cache.ignite;

import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration;
import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.convert.ConversionService;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory class that creates a {@link org.apache.ignite.client.IgniteClient}
 *
 * @author Michael Pollind
 */
@Factory
public class IgniteCacheFactory implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(IgniteCacheFactory.class);
    private List<Ignite> sessions = new ArrayList<>(2);

    @EachBean(IgniteClientConfiguration.class)
    Ignite igniteClient(IgniteClientConfiguration configuration) {
        return Ignition.start(configuration.getConfiguration());
    }

    @EachBean(IgniteCacheConfiguration.class)
    IgniteSyncCache syncCache(ConversionService<?> service, IgniteCacheConfiguration configuration, Collection<Ignite> ignites) throws Exception {
        for (Ignite ign : ignites) {
            if (ign.name().equals(configuration.getClient())) {
                return new IgniteSyncCache(service, ign.createCache(configuration.getConfiguration()));
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
                    LOG.warn("Error closing data source [" + sess + "]: " + e.getMessage(), e);
                }
            }
        }
    }


}
