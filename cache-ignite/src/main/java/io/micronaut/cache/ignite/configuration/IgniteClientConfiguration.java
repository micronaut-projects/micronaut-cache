package io.micronaut.cache.ignite.configuration;


import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

@EachProperty(IgniteClientConfiguration.PREFIX)
public class IgniteClientConfiguration {
    public static final String PREFIX = "ignite.clients";

    private final String name;

    @ConfigurationBuilder
    private org.apache.ignite.configuration.IgniteConfiguration configuration = new org.apache.ignite.configuration.IgniteConfiguration();

    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    public org.apache.ignite.configuration.IgniteConfiguration getConfiguration() {
        return configuration;
    }

}
