package io.micronaut.cache.ehcache;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import org.ehcache.config.builders.CacheManagerBuilder;

import static io.micronaut.cache.ehcache.EhcacheManagerConfiguration.PREFIX;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@ConfigurationProperties(PREFIX)
public class EhcacheManagerConfiguration {

    public static final String PREFIX = "ehcache";

    @ConfigurationBuilder(prefixes = "with")
    CacheManagerBuilder builder = CacheManagerBuilder.newCacheManagerBuilder();

}
