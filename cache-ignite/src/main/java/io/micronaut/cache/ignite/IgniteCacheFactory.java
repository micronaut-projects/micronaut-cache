package io.micronaut.cache.ignite;

import io.micronaut.cache.ignite.configuration.IgniteCacheConfiguration;
import io.micronaut.cache.ignite.configuration.IgniteClientConfiguration;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    public IgniteCacheFactory() {
    }

    @EachBean(IgniteClientConfiguration.class)
    Ignite igniteClient(IgniteClientConfiguration clientConfiguration) {
        Ignite ignite = Ignition.start(clientConfiguration.getConfiguration());

        sessions.add(ignite);
        return ignite;
    }


    @EachBean(IgniteCacheConfiguration.class)
    IgniteSyncCache syncCache(ConversionService<?> service,IgniteCacheConfiguration configuration, BeanContext beanContext) {
        Ignite ignite = beanContext.getBean(Ignite.class, Qualifiers.byName(configuration.getClient()));
        return new IgniteSyncCache(service, ignite.createCache(configuration.getConfiguration()));
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
