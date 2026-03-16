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
package io.micronaut.cache.oracle.serialization;

import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.core.beans.BeanMap;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serializes generated cache keys into canonical JSON payload bytes and SHA-256 hash bytes.
 *
 * @author Davide Cocco
 * @since 5.0.0
 */
public final class OracleKeySerializer {

    private final JsonMapper jsonMapper;
    private final ConversionService conversionService;

    public OracleKeySerializer(ConversionService conversionService) {
        this(JsonMapper.createDefault(), conversionService);
    }

    public OracleKeySerializer(JsonMapper jsonMapper, ConversionService conversionService) {
        this.jsonMapper = jsonMapper;
        this.conversionService = conversionService;
    }

    public OracleCacheKey serialize(Object cacheKey) {
        List<Object> arguments = extractArguments(cacheKey);
        byte[] payload = toJsonBytes(canonicalize(arguments));
        return new OracleCacheKey(sha256(payload), payload);
    }

    private List<Object> extractArguments(Object cacheKey) {
        if (cacheKey == ParametersKey.ZERO_ARG_KEY) {
            return List.of();
        }
        if (cacheKey instanceof ParametersKey parametersKey) {
            return List.of(getParams(parametersKey));
        }
        return List.of(cacheKey);
    }

    private Object[] getParams(ParametersKey key) {
        try {
            var field = ParametersKey.class.getDeclaredField("params");
            field.setAccessible(true);
            return (Object[]) field.get(key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read ParametersKey arguments", e);
        }
    }

    private Object canonicalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor) {
            return String.valueOf(value);
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString();
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> normalized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                normalized.add(canonicalize(java.lang.reflect.Array.get(value, i)));
            }
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>(collection.size());
            for (Object entry : collection) {
                normalized.add(canonicalize(entry));
            }
            return normalized;
        }
        if (value instanceof Map<?, ?> map) {
            return canonicalizeMap(map);
        }

        Map<String, Object> beanMap = tryBeanMap(value);
        if (beanMap != null) {
            return beanMap;
        }

        Optional<String> converted = conversionService.convert(value, String.class);
        return converted.orElseGet(() -> String.valueOf(value));
    }

    private Map<String, Object> canonicalizeMap(Map<?, ?> map) {
        List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));

        Map<String, Object> normalized = new LinkedHashMap<>(entries.size());
        for (Map.Entry<?, ?> entry : entries) {
            normalized.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
        }
        return normalized;
    }

    private Map<String, Object> tryBeanMap(Object value) {
        try {
            BeanMap<Object> beanMap = BeanMap.of(value);
            List<String> keys = new ArrayList<>(beanMap.keySet());
            keys.sort(String::compareTo);
            Map<String, Object> normalized = new LinkedHashMap<>(keys.size());
            for (String key : keys) {
                normalized.put(key, canonicalize(beanMap.get(key)));
            }
            return normalized;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
