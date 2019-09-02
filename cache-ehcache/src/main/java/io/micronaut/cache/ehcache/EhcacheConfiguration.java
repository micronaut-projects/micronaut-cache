package io.micronaut.cache.ehcache;

import io.micronaut.cache.ehcache.serialization.CharSequenceSerializer;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.naming.Named;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import javax.annotation.Nonnull;

import java.io.Serializable;

import static io.micronaut.cache.ehcache.EhcacheConfiguration.PREFIX;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@EachProperty(PREFIX)
public class EhcacheConfiguration implements Named {

    public static final String PREFIX = EhcacheManagerConfiguration.PREFIX + ".caches";
    public static final Integer DEFAULT_MAX_ENTRIES = 10;
    public static final Class<?> DEFAULT_KEY_TYPE = Serializable.class;
    public static final Class<?> DEFAULT_VALUE_TYPE = Serializable.class;

    private final String name;

    private Integer maxEntries = DEFAULT_MAX_ENTRIES;
    private Class<?> keyType = DEFAULT_KEY_TYPE;
    private Class<?> valueType = DEFAULT_VALUE_TYPE;

    @ConfigurationBuilder(prefixes = "with")
    CacheConfigurationBuilder builder;


    public EhcacheConfiguration(@Parameter String name) {
        this.name = name;
        initBuilder();
    }

    private void initBuilder() {
        //TODO allow customisations
        this.builder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(keyType, valueType, ResourcePoolsBuilder.heap(maxEntries));
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    public Integer getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Class<?> getKeyType() {
        return keyType;
    }

    public void setKeyType(Class<?> keyType) {
        this.keyType = keyType;
        initBuilder();
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public void setValueType(Class<?> valueType) {
        this.valueType = valueType;
        initBuilder();
    }
}
