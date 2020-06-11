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
package io.micronaut.cache.ignite.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Optional;

/**
 * Ignite configuration.
 */
@EachProperty(value = "ignite.clients", primary = "default")
public class IgniteClientConfiguration implements Named {
    private final String name;

    @ConfigurationBuilder(excludes = {"Name"})
    private final IgniteConfiguration configuration = new IgniteConfiguration();
    private IgniteDiscoveryConfiguration discovery = new IgniteDiscoveryConfiguration();

    /**
     * @param name Name or key of the client.
     */
    public IgniteClientConfiguration(@Parameter String name) {
        this.name = name;
    }

    /**
     *
     * @return the ignite configuration.
     */
    public IgniteConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return the discovery configuration.
     */
    public IgniteDiscoveryConfiguration getDiscovery() {
        return discovery;
    }

    /**
     *
     * @param discovery the discovery configuration.
     */
    public void setDiscovery(IgniteDiscoveryConfiguration discovery) {
        this.discovery = discovery;
    }

    /**
     * @return the configuration builder.
     */
    public IgniteConfiguration build() {
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration(configuration).
            setIgniteInstanceName(name);
        discovery.buildDiscovery().ifPresent(igniteConfiguration::setDiscoverySpi);
        return igniteConfiguration;
    }

    @NonNull
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Discovery configuration.
     */
    @ConfigurationProperties("discovery")
    public static class IgniteDiscoveryConfiguration {
        DiscoveryMulticastIpFinder multicast;
        SharedFilesystemIpFinder filesystem;
        VmIpFinder vm;

        /**
         * @param multicast the multicast configuration.
         */
        public void setMulticast(DiscoveryMulticastIpFinder multicast) {
            this.multicast = multicast;
        }

        /**
         * @return multicast configuration.
         */
        public DiscoveryMulticastIpFinder getMulticast() {
            return multicast;
        }

        /**
         * @return shared filesystem configuration.
         */
        public SharedFilesystemIpFinder getFilesystem() {
            return filesystem;
        }

        /**
         * @param filesystem shared filesystem configuration.
         */
        public void setFilesystem(SharedFilesystemIpFinder filesystem) {
            this.filesystem = filesystem;
        }

        /**
         * @return fixed ip configuration.
         */
        public VmIpFinder getVm() {
            return vm;
        }

        /**
         * @param vm fixed ip configuration.
         */
        public void setVm(VmIpFinder vm) {
            this.vm = vm;
        }

        /**
         * @return build Ignite configuration from base and properties.
         */
        public Optional<TcpDiscoverySpi> buildDiscovery() {
            if (multicast != null) {
                return Optional.of(new TcpDiscoverySpi().setIpFinder(multicast.getConfiguration()));
            }
            if (vm != null) {
                return Optional.of(new TcpDiscoverySpi().setIpFinder(vm.getConfiguration()));
            }
            if (filesystem != null) {
                return Optional.of(new TcpDiscoverySpi().setIpFinder(filesystem.getConfiguration()));
            }
            return Optional.empty();
        }

        /**
         * {@link TcpDiscoveryMulticastIpFinder} configuration.
         */
        @ConfigurationProperties("multicast")
        public static class DiscoveryMulticastIpFinder {
            @ConfigurationBuilder
            private TcpDiscoveryMulticastIpFinder configuration = new TcpDiscoveryMulticastIpFinder();

            /**
             * @return multicast ip finder configuration
             */
            public TcpDiscoveryMulticastIpFinder getConfiguration() {
                return configuration;
            }
        }

        /**
         * {@link TcpDiscoverySharedFsIpFinder} configuration.
         */
        @ConfigurationProperties("filesystem")
        public static class SharedFilesystemIpFinder {
            @ConfigurationBuilder
            private TcpDiscoverySharedFsIpFinder configuration = new TcpDiscoverySharedFsIpFinder();

            /**
             * @return Filesystem Ip finder configuration.
             */
            public TcpDiscoverySharedFsIpFinder getConfiguration() {
                return configuration;
            }

        }

        /**
         * {@link TcpDiscoveryVmIpFinder} configuration.
         */
        @ConfigurationProperties("vm")
        public static class VmIpFinder {
            @ConfigurationBuilder
            TcpDiscoveryVmIpFinder configuration = new TcpDiscoveryVmIpFinder();

            /**
             * @return Fixed Ip finder configuration.
             */
            public TcpDiscoveryVmIpFinder getConfiguration() {
                return configuration;
            }

        }
    }
}
