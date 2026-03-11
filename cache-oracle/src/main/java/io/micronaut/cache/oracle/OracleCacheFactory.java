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
package io.micronaut.cache.oracle;

import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration;
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository;
import io.micronaut.cache.oracle.serialization.OracleKeySerializer;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;

/**
 * Creates Oracle cache beans for configured cache names.
 */
@Factory
public final class OracleCacheFactory {

    @Singleton
    static OracleKeySerializer oracleKeySerializer(JsonMapper jsonMapper, ConversionService conversionService) {
        return new OracleKeySerializer(jsonMapper, conversionService);
    }

    @EachBean(OracleCacheConfiguration.class)
    static OracleSyncCache oracleSyncCache(@Parameter OracleCacheConfiguration configuration,
                                           OracleCacheEntryRepository entryRepository,
                                           OracleKeySerializer keySerializer,
                                           JsonMapper jsonMapper) {
        return new OracleSyncCache(configuration, entryRepository, keySerializer, jsonMapper);
    }
}
