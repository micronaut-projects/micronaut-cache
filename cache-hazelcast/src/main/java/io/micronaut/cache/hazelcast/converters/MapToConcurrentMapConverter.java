package io.micronaut.cache.hazelcast.converters;


import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class MapToConcurrentMapConverter implements TypeConverter<Map, ConcurrentMap> {

    @Override
    public Optional<ConcurrentMap> convert(Map object, Class<ConcurrentMap> targetType, ConversionContext context) {
        return Optional.of(new ConcurrentHashMap(object));
    }
}
