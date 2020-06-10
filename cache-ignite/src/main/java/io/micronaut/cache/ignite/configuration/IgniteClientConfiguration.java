package io.micronaut.cache.ignite.configuration;


import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.jdbc.TcpDiscoveryJdbcIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Optional;

@EachProperty(value = "ignite.clients", primary = "default")
public class IgniteClientConfiguration {
    private final String name;

    @ConfigurationBuilder(excludes = {"Name"})
    private IgniteConfiguration configuration = new IgniteConfiguration();

    private DiscoveryMulticastIpFinder multicastIpfinder;
    private SharedFilesystemIpFinder filesystemIpfinder;
    private JdbcIpFinder jdbcIpfinder;
    private VmIpFinder vmIpfinder;

    public void setMulticastIpfinder(DiscoveryMulticastIpFinder multicastIpfinder) {
        this.multicastIpfinder = multicastIpfinder;
    }

    public void setFilesystemIpfinder(SharedFilesystemIpFinder filesystemIpfinder) {
        this.filesystemIpfinder = filesystemIpfinder;
    }

    public void setJdbcIpfinder(JdbcIpFinder jdbcIpfinder) {
        this.jdbcIpfinder = jdbcIpfinder;
    }

    public void setVmIpfinder(VmIpFinder vmIpfinder) {
        this.vmIpfinder = vmIpfinder;
    }

    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    public IgniteConfiguration getConfiguration() {
        return configuration;
    }


    public IgniteConfiguration build() {
        IgniteConfiguration igniteConfiguration = configuration.
            setIgniteInstanceName(name);
        BuildDiscovery().ifPresent((discoverySpi) -> igniteConfiguration.setDiscoverySpi(discoverySpi));
        return igniteConfiguration;
    }


    @ConfigurationProperties("multicast-ipfinder")
    public static class DiscoveryMulticastIpFinder {
        @ConfigurationBuilder
        private TcpDiscoveryMulticastIpFinder configuration = new TcpDiscoveryMulticastIpFinder();

        public TcpDiscoveryMulticastIpFinder getConfiguration() {
            return configuration;
        }
    }

    @ConfigurationProperties("filesystem-ipfinder")
    public static class SharedFilesystemIpFinder {
        @ConfigurationBuilder
        private TcpDiscoverySharedFsIpFinder configuration = new TcpDiscoverySharedFsIpFinder();

        public TcpDiscoverySharedFsIpFinder getConfiguration() {
            return configuration;
        }

    }

    @ConfigurationProperties("jdbc-ipfinder")
    public static class JdbcIpFinder {
        @ConfigurationBuilder
        TcpDiscoveryJdbcIpFinder configuration = new TcpDiscoveryJdbcIpFinder();

        public TcpDiscoveryJdbcIpFinder getConfiguration() {
            return configuration;
        }

    }

    @ConfigurationProperties("vm-ipfinder")
    public static class VmIpFinder {
        @ConfigurationBuilder
        TcpDiscoveryVmIpFinder configuration = new TcpDiscoveryVmIpFinder();

        public TcpDiscoveryVmIpFinder getConfiguration() {
            return configuration;
        }

    }

    public Optional<TcpDiscoverySpi> BuildDiscovery() {
        if (multicastIpfinder != null)
            return Optional.of(new TcpDiscoverySpi().setIpFinder(multicastIpfinder.getConfiguration()));
        if (vmIpfinder != null)
            return Optional.of(new TcpDiscoverySpi().setIpFinder(vmIpfinder.getConfiguration()));
        if (jdbcIpfinder != null)
            return Optional.of(new TcpDiscoverySpi().setIpFinder(jdbcIpfinder.getConfiguration()));
        if (filesystemIpfinder != null)
            return Optional.of(new TcpDiscoverySpi().setIpFinder(filesystemIpfinder.getConfiguration()));
        return Optional.empty();
    }

}
