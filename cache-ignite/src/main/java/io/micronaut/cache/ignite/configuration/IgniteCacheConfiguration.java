package io.micronaut.cache.ignite.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.apache.ignite.configuration.CacheConfiguration;

@EachProperty(IgniteCacheConfiguration.PREFIX)
public class IgniteCacheConfiguration implements Named {
    public static final String PREFIX = "ignite.caches";

    private final String name;
    private String client = "default";

    @ConfigurationBuilder(excludes = {"Name"})
    private CacheConfiguration configuration = new CacheConfiguration();

    public IgniteCacheConfiguration(@Parameter String name) {
        this.name = name;
    }

    public CacheConfiguration getConfiguration() {
        return configuration.setName(this.name);
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClient() {
        return client;
    }

    @NonNull
    @Override
    public String getName() {
        return this.name;
    }
}
