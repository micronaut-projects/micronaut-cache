/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.cache.caffeine.graal;

import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A native image feature that configures common Caffeine cache implementations for reflection.
 * It is not a complete list, and users using less common implementations will need to provide their own configuration.
 *
 * @author Tim Yates
 * @since 4.0.0
 */
@Internal
public class CaffeineFeature implements Feature {

    private static final CacheType[] COMMON_CACHE_TYPES = new CacheType[]{
        new CacheType("com.github.benmanes.caffeine.cache.PDMS"),
        new CacheType("com.github.benmanes.caffeine.cache.PSA"),
        new CacheType("com.github.benmanes.caffeine.cache.PSAW"),
        new CacheType("com.github.benmanes.caffeine.cache.PS", "key", "value"),
        new CacheType("com.github.benmanes.caffeine.cache.PSW", "writeTime"),
        new CacheType("com.github.benmanes.caffeine.cache.PSMS"),
        new CacheType("com.github.benmanes.caffeine.cache.PSWMS"),
        new CacheType("com.github.benmanes.caffeine.cache.PSWMW"),
        new CacheType("com.github.benmanes.caffeine.cache.SILMS"),
        new CacheType("com.github.benmanes.caffeine.cache.SSA"),
        new CacheType("com.github.benmanes.caffeine.cache.SSAW"),
        new CacheType("com.github.benmanes.caffeine.cache.SSLA"),
        new CacheType("com.github.benmanes.caffeine.cache.SSLMS"),
        new CacheType("com.github.benmanes.caffeine.cache.SSMS"),
        new CacheType("com.github.benmanes.caffeine.cache.SSMSA"),
        new CacheType("com.github.benmanes.caffeine.cache.SSMSW"),
        new CacheType("com.github.benmanes.caffeine.cache.SSW"),
    };

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeClassInitialization.initializeAtRunTime("io.micronaut.cache.caffeine.metrics.$CaffeineCacheMetricsBinder$Definition");

        RuntimeClassInitialization.initializeAtBuildTime("com.github.benmanes.caffeine.cache.RemovalCause");
        registerFields(access, "com.github.benmanes.caffeine.cache.BLCHeader$DrainStatusRef", "drainStatus");
        registerFields(access, "com.github.benmanes.caffeine.cache.BBHeader$ReadCounterRef", "readCounter");
        registerFields(access, "com.github.benmanes.caffeine.cache.BBHeader$ReadAndWriteCounterRef", "writeCounter");
        registerFields(access, "com.github.benmanes.caffeine.cache.StripedBuffer", "tableBusy");
        registerFields(access, "java.lang.Thread", "threadLocalRandomProbe");

        for (CacheType commonCacheType : COMMON_CACHE_TYPES) {
            registerFieldsAndDeclaredConstructors(access, commonCacheType.className, commonCacheType.fields);
        }
    }

    private void registerFieldsAndDeclaredConstructors(BeforeAnalysisAccess access, String clz, String... fields) {
        RuntimeReflection.register(access.findClassByName(clz));
        RuntimeReflection.register(access.findClassByName(clz).getDeclaredConstructors());
        registerFields(access, clz, fields);
    }

    private void registerFields(BeforeAnalysisAccess access, String clz, String... fields) {
        for (Field field : access.findClassByName(clz).getDeclaredFields()) {
            if (Arrays.asList(fields).contains(field.getName())) {
                RuntimeReflection.register(field);
            }
        }
    }

    @SuppressWarnings({
        "java:S6218",           // We don't do comparison on this holder type
        "checkstyle:MethodName" // Checkstyle thinks this is a method...
    })
    private record CacheType(String className, String... fields) {
    }
}
