package io.micronaut.cache.ignite.configuration;


import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Optional;

@EachProperty(value = "ignite.clients", primary = "default")
public class IgniteClientConfiguration {
    private final String name;

    @ConfigurationBuilder(excludes = {"Name"})
    private IgniteConfiguration configuration = new IgniteConfiguration();
    public IgniteDiscoveryConfiguration discovery = new IgniteDiscoveryConfiguration();

    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    public IgniteConfiguration getConfiguration() {
        return configuration;
    }

    public void setDiscovery(IgniteDiscoveryConfiguration discovery) {
        this.discovery = discovery;
    }

    public IgniteDiscoveryConfiguration getDiscovery() {
        return discovery;
    }

    public IgniteConfiguration build() {
        IgniteConfiguration igniteConfiguration = configuration.
            setIgniteInstanceName(name);
        discovery.BuildDiscovery().ifPresent(igniteConfiguration::setDiscoverySpi);
        return igniteConfiguration;
    }

    @ConfigurationProperties("discovery")
    public static class IgniteDiscoveryConfiguration {
        DiscoveryMulticastIpFinder multicast;
        SharedFilesystemIpFinder filesystem;
        //        JdbcIpFinder jdbc;
        VmIpFinder vm;

        public void setMulticast(DiscoveryMulticastIpFinder multicast) {
            this.multicast = multicast;
        }

        public DiscoveryMulticastIpFinder getMulticast() {
            return multicast;
        }

        public SharedFilesystemIpFinder getFilesystem() {
            return filesystem;
        }

        public void setFilesystem(SharedFilesystemIpFinder filesystem) {
            this.filesystem = filesystem;
        }

//        public void setJdbc(JdbcIpFinder jdbc) {
//            this.jdbc = jdbc;
//        }

//        public JdbcIpFinder getJdbc() {
//            return jdbc;
//        }

        public VmIpFinder getVm() {
            return vm;
        }

        public void setVm(VmIpFinder vm) {
            this.vm = vm;
        }

        @ConfigurationProperties("multicast")
        public static class DiscoveryMulticastIpFinder {
            @ConfigurationBuilder
            private TcpDiscoveryMulticastIpFinder configuration = new TcpDiscoveryMulticastIpFinder();

            public TcpDiscoveryMulticastIpFinder getConfiguration() {
                return configuration;
            }
        }

        @ConfigurationProperties("filesystem")
        public static class SharedFilesystemIpFinder {
            @ConfigurationBuilder
            private TcpDiscoverySharedFsIpFinder configuration = new TcpDiscoverySharedFsIpFinder();

            public TcpDiscoverySharedFsIpFinder getConfiguration() {
                return configuration;
            }

        }

//        @ConfigurationProperties("jdbc")
//        public static class JdbcIpFinder {
//            @ConfigurationBuilder
//            TcpDiscoveryJdbcIpFinder configuration = new TcpDiscoveryJdbcIpFinder();
//
//            public TcpDiscoveryJdbcIpFinder getConfiguration() {
//                return configuration;
//            }
//
//        }

        @ConfigurationProperties("vm")
        public static class VmIpFinder {
            @ConfigurationBuilder
            TcpDiscoveryVmIpFinder configuration = new TcpDiscoveryVmIpFinder();

            public TcpDiscoveryVmIpFinder getConfiguration() {
                return configuration;
            }

        }

        public Optional<TcpDiscoverySpi> BuildDiscovery() {
            if (multicast != null)
                return Optional.of(new TcpDiscoverySpi().setIpFinder(multicast.getConfiguration()));
            if (vm != null)
                return Optional.of(new TcpDiscoverySpi().setIpFinder(vm.getConfiguration()));
//            if (jdbc != null)
//                return Optional.of(new TcpDiscoverySpi().setIpFinder(jdbc.getConfiguration()));
            if (filesystem != null)
                return Optional.of(new TcpDiscoverySpi().setIpFinder(filesystem.getConfiguration()));
            return Optional.empty();
        }

    }
}
