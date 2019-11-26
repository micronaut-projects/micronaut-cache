package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@ConfigurationProperties("infinispan.client.hotrod")
public class InfinispanHotRodClientConfiguration {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final Integer DEFAULT_PORT = 11222;

    @io.micronaut.context.annotation.ConfigurationBuilder
    private ConfigurationBuilder builder;

    private String host = DEFAULT_HOST;
    private Integer port = DEFAULT_PORT;
    private StatisticsConfiguration statistics = new StatisticsConfiguration();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public StatisticsConfiguration getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsConfiguration statistics) {
        this.statistics = statistics;
    }

    public ConfigurationBuilder getBuilder() {
        if (builder == null) {
            builder = new ConfigurationBuilder();
        }
        this.builder
                .marshaller(new ProtoStreamMarshaller())
                .addServer()
                    .host(host)
                    .port(port);
        if (statistics.isEnabled()) {
            this.builder
                    .statistics()
                        .enable()
                        .jmxDomain(statistics.jmxDomain);
        }
        return builder;
    }

    @ConfigurationProperties("statistics")
    static class StatisticsConfiguration implements Toggleable {

        private static final boolean DEFAULT_ENABLED = false;

        private boolean enabled = DEFAULT_ENABLED;
        private String jmxDomain;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJmxDomain() {
            return jmxDomain;
        }

        public void setJmxDomain(String jmxDomain) {
            this.jmxDomain = jmxDomain;
        }
    }
}
