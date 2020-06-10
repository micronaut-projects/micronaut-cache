package io.micronaut.cache.ignite.configuration;


import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

@EachProperty(IgniteClientConfiguration.PREFIX)
public class IgniteClientConfiguration {
    public static final String PREFIX = "ignite.clients";

    private final String name;

    @ConfigurationBuilder
    IgniteConfiguration configuration = new IgniteConfiguration();

    @ConfigurationBuilder(prefixes = "finder")
    TcpDiscoveryMulticastIpFinder finder = new TcpDiscoveryMulticastIpFinder();


    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    public org.apache.ignite.configuration.IgniteConfiguration getConfiguration() {
        return configuration.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(finder));
    }

}
