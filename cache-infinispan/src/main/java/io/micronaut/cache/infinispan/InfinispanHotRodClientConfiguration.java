/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.io.ResourceResolver;
import org.infinispan.client.hotrod.configuration.*;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

/**
 * Infinispan HotRod client configuration properties.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 * @see <a href="https://infinispan.org/docs/stable/titles/hotrod_java/hotrod_java.html#hotrod_java_client">Infinispan reference</a>
 */
@ConfigurationProperties(InfinispanHotRodClientConfiguration.PREFIX)
public class InfinispanHotRodClientConfiguration {

    public static final String PREFIX = "infinispan.client.hotrod";

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_CONFIG_FILE = "classpath:hotrod-client.properties";

    private static final Logger LOG = LoggerFactory.getLogger(InfinispanHotRodClientConfiguration.class);

    private ResourceResolver resourceResolver;
    private ExecutorFactory executorFactory;

    @ConfigurationBuilder(prefixes = {"set", ""}, includes = {"addCluster", "addServers", "balancingStrategy", "connectionTimeout", "forceReturnValues", "keySizeEstimate", "marshaller", "addContextInitializer", "protocolVersion", "socketTimeout", "tcpNoDelay", "tcpKeepAlive", "valueSizeEstimate", "maxRetries", "batchSize"})
    private org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();

    @ConfigurationBuilder(value = "server", prefixes = {"set", ""}, includes = {"host", "port"})
    private ServerConfigurationBuilder server = builder.addServer();

    @ConfigurationBuilder(value = "statistics", prefixes = {"set", ""}, includes = {"enabled", "jmxEnabled", "jmxDomain", "jmxName"})
    private StatisticsConfigurationBuilder statistics = builder.statistics();

    @ConfigurationBuilder(value = "connection-pool", prefixes = {"set", ""}, includes = {"maxActive", "maxWait", "minIdle", "minEvictableIdleTime", "maxPendingRequests"})
    private ConnectionPoolConfigurationBuilder connectionPool = builder.connectionPool();

    @ConfigurationBuilder(value = "async-executor-factory", prefixes = {"set", ""}, includes = {"factoryClass"})
    private ExecutorFactoryConfigurationBuilder asyncExecutorFactory = builder.asyncExecutorFactory();

    @ConfigurationBuilder(value = "security.authentication", prefixes = {"set", ""}, includes = {"enabled", "saslMechanism", "serverName", "username", "password", "realm"})
    private AuthenticationConfigurationBuilder authentication = builder.security().authentication();

    @ConfigurationBuilder(value = "security.ssl", prefixes = {"set", ""}, includes = {"enabled", "keyStoreFileName", "keyStoreType", "keyStorePassword", "keyStoreCertificatePassword", "keyAlias", "trustStoreFileName", "trustStorePath", "trustStoreType", "trustStorePassword", "sniHostName", "protocol"})
    private SslConfigurationBuilder ssl = builder.security().ssl();

    @ConfigurationBuilder(value = "near-cache", prefixes = {"set", ""}, includes = {"maxEntries", "cacheNamePattern"})
    private NearCacheConfigurationBuilder nearCache = builder.nearCache();

    private String configFile = DEFAULT_CONFIG_FILE;

    /**
     * @param resourceResolver the resource resolver
     * @param executorFactory the executor factory
     */
    public InfinispanHotRodClientConfiguration(ResourceResolver resourceResolver, ExecutorFactory executorFactory) {
        this.resourceResolver = resourceResolver;
        this.executorFactory = executorFactory;
    }

    /**
     * @return the server configuration builder
     */
    public ServerConfigurationBuilder getServer() {
        return server;
    }

    /**
     * @return the statistics builder
     */
    public StatisticsConfigurationBuilder getStatistics() {
        return statistics;
    }

    /**
     * @return the connection pool builder
     */
    public ConnectionPoolConfigurationBuilder getConnectionPool() {
        return connectionPool;
    }

    /**
     * @return the executor factory builder
     */
    public ExecutorFactoryConfigurationBuilder getAsyncExecutorFactory() {
        return asyncExecutorFactory;
    }

    /**
     * @return the authentication builder
     */
    public AuthenticationConfigurationBuilder getAuthentication() {
        return authentication;
    }

    /**
     * @return the SSL builder
     */
    public SslConfigurationBuilder getSsl() {
        return ssl;
    }

    /**
     * @return the near cache builder
     */
    public NearCacheConfigurationBuilder getNearCache() {
        return nearCache;
    }

    /**
     * @return the configuration file location
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * @param configFile the configuration file location
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @return the Infinispan configuration builder that will be used to create a remote cache manager
     */
    public org.infinispan.client.hotrod.configuration.ConfigurationBuilder getBuilder() {
        if (configFile != null) {
            Optional<URL> configResource = resourceResolver.getResource(configFile);
            if (configResource.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Infinispan configuration from file: {}", configFile);
                }
                Properties properties = new Properties();
                try (Reader r = new FileReader(Paths.get(configResource.get().toURI()).toFile())) {
                    properties.load(r);
                    builder.withProperties(properties);
                } catch (IOException | URISyntaxException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("An error has ocurred while reading the configuration file [{}]: {}", configFile, e.getMessage());
                    }
                }
            }
        }

        if (server.create().host() == null) {
            server.host(DEFAULT_HOST);
        }

        if (DefaultAsyncExecutorFactory.class.equals(asyncExecutorFactory.create().factoryClass())) {
            asyncExecutorFactory.factory(executorFactory);
        }

        return builder;
    }

}
