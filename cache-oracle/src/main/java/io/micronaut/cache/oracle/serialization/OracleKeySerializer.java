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
package io.micronaut.cache.oracle.serialization;

import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.serde.oracle.jdbc.json.OracleJdbcJsonBinaryObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * Serializes generated cache keys into canonical JSON payload bytes and SHA-256 hash bytes.
 *
 * @author Davide Cocco
 * @since 6.2.0
 */
public final class OracleKeySerializer {

    private final OracleJdbcJsonBinaryObjectMapper jsonMapper;

    public OracleKeySerializer(OracleJdbcJsonBinaryObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public OracleCacheKey serialize(Object cacheKey) {
        byte[] payload = toJsonBytes(extractArguments(cacheKey));
        return new OracleCacheKey(sha256(payload), payload);
    }

    private List<Object> extractArguments(Object cacheKey) {
        if (cacheKey == ParametersKey.ZERO_ARG_KEY) {
            return List.of();
        }
        if (cacheKey instanceof ParametersKey parametersKey) {
            return Arrays.asList(parametersKey.getParameters());
        }
        return List.of(cacheKey);
    }

    private byte[] sha256(byte[] payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private byte[] toJsonBytes(Object value) {
        try {
            return jsonMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize cache key payload", e);
        }
    }
}
