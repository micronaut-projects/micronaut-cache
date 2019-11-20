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
package io.micronaut.cache.hazelcast.converters;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Converts a map to a concurrent map.
 *
 * @author James Kleeh
 * @since 1.0.0
 */
@Singleton
public class MapToConcurrentMapConverter implements TypeConverter<Map, ConcurrentMap> {

    @Override
    public Optional<ConcurrentMap> convert(Map object, Class<ConcurrentMap> targetType, ConversionContext context) {
        return Optional.of(new ConcurrentHashMap(object));
    }
}
