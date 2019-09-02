package io.micronaut.cache.ehcache;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.ehcache.CacheManager;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Factory
public class EhcacheManagerFactory {

    private EhcacheManagerConfiguration configuration;

    public EhcacheManagerFactory(EhcacheManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Bean
    public CacheManager cacheManager() {
        return configuration.builder.build(true);
    }
}
