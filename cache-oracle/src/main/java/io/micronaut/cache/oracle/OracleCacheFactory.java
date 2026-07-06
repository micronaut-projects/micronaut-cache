/*
 * Copyright 2017-2026 original authors
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
import io.micronaut.cache.oracle.persistence.OracleCacheStatsRepository;
import io.micronaut.cache.oracle.serialization.OracleKeySerializer;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.serde.SerdeRegistry;
import io.micronaut.serde.config.SerdeConfiguration;
import io.micronaut.serde.config.SerializationConfiguration;
import io.micronaut.serde.config.annotation.SerdeConfig;
import io.micronaut.serde.oracle.jdbc.json.OracleJdbcJsonBinaryObjectMapper;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;

/**
 * Creates Oracle cache beans for configured cache names.
 *
 * @author Davide Cocco
 * @since 6.2.0
 */
@Factory
final class OracleCacheFactory {

    /**
     * Builds the Oracle cache key serializer.
     *
     * @param serdeRegistry The Serde registry
     * @param serdeConfiguration The Serde configuration
     * @param serializationConfiguration The Serde serialization configuration
     * @return The key serializer
     */
    @Singleton
    static OracleKeySerializer oracleKeySerializer(SerdeRegistry serdeRegistry,
                                                   SerdeConfiguration serdeConfiguration,
                                                   SerializationConfiguration serializationConfiguration) {
        SerdeRegistry keySerdeRegistry = serdeRegistry.cloneWithConfiguration(
            null,
            new OracleKeySerializationConfiguration(serializationConfiguration),
            null
        );
        OracleJdbcJsonBinaryObjectMapper keyJsonMapper = new OracleJdbcJsonBinaryObjectMapper(
            keySerdeRegistry,
            serdeConfiguration
        );
        return new OracleKeySerializer(keyJsonMapper);
    }

    @EachBean(OracleCacheConfiguration.class)
    static OracleSyncCache oracleSyncCache(@Parameter OracleCacheConfiguration configuration,
                                           OracleCacheEntryRepository entryRepository,
                                           OracleCacheStatsRepository statsRepository,
                                           @Named(TaskExecutors.BLOCKING) ExecutorService ioExecutor,
                                           OracleKeySerializer keySerializer,
                                           OracleJdbcJsonBinaryObjectMapper jsonMapper) {
        return new OracleSyncCache(configuration, entryRepository, statsRepository, ioExecutor, keySerializer, jsonMapper);
    }

    private record OracleKeySerializationConfiguration(
        SerializationConfiguration delegate
    ) implements SerializationConfiguration {

        @Override
        public SerdeConfig.SerInclude getInclusion() {
            return delegate.getInclusion();
        }

        @Override
        public boolean isAlwaysSerializeErrorsAsList() {
            return delegate.isAlwaysSerializeErrorsAsList();
        }

        @Override
        public boolean sortPropertiesAlphabetically() {
            return true;
        }

        @Override
        public boolean writeDateTimestampsAsNanoseconds() {
            return delegate.writeDateTimestampsAsNanoseconds();
        }

        @Override
        public boolean writeDatesWithZoneId() {
            return delegate.writeDatesWithZoneId();
        }

        @Override
        public boolean writeSingleElemArraysUnwrapped() {
            return delegate.writeSingleElemArraysUnwrapped();
        }

        @Override
        public boolean writeSortedMapEntries() {
            return true;
        }

        @Override
        public boolean disableGeneratedSerializer() {
            return delegate.disableGeneratedSerializer();
        }
    }
}
