package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.io.ResourceResolver;
import org.infinispan.client.hotrod.configuration.*;
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
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@ConfigurationProperties("infinispan.client.hotrod")
public class InfinispanHotRodClientConfiguration {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_CONFIG_FILE = "classpath:hotrod-client.properties";

    private static final Logger LOG = LoggerFactory.getLogger(InfinispanHotRodClientConfiguration.class);

    private ResourceResolver resourceResolver;

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

    public InfinispanHotRodClientConfiguration(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public ServerConfigurationBuilder getServer() {
        return server;
    }

    public StatisticsConfigurationBuilder getStatistics() {
        return statistics;
    }

    public ConnectionPoolConfigurationBuilder getConnectionPool() {
        return connectionPool;
    }

    public ExecutorFactoryConfigurationBuilder getAsyncExecutorFactory() {
        return asyncExecutorFactory;
    }

    public AuthenticationConfigurationBuilder getAuthentication() {
        return authentication;
    }

    public SslConfigurationBuilder getSsl() {
        return ssl;
    }

    public NearCacheConfigurationBuilder getNearCache() {
        return nearCache;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public org.infinispan.client.hotrod.configuration.ConfigurationBuilder getBuilder() {
        if (configFile != null) {
            Optional<URL> configResource = resourceResolver.getResource(configFile);
            if (configResource.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading Infinispan configuration from file: {}", configFile);
                }
                Properties properties = new Properties();
                try(Reader r = new FileReader(Paths.get(configResource.get().toURI()).toFile())) {
                    properties.load(r);
                    builder.withProperties(properties);
                } catch (IOException | URISyntaxException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("An error has ocurred while reading the configuration file [{}]: {}", configFile, e.getMessage());
                    }
                }
            }
        }

        if (this.server.create().host() == null) {
            this.server.host(DEFAULT_HOST);
        }

        builder
                .connectionPool().read(connectionPool.create())
                .asyncExecutorFactory().read(asyncExecutorFactory.create())
                .statistics().read(statistics.create());

        SecurityConfigurationBuilder security = builder.security();
        security.authentication().read(authentication.create());
        security.ssl().read(ssl.create());
        builder.security().read(security.create());

        builder.nearCache().read(nearCache.create());

        return builder;
    }

}
