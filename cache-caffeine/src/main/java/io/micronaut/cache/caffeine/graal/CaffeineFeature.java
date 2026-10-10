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
package io.micronaut.cache.caffeine.graal;

import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A native image feature that configures Caffeine for reflection.
 * <p>
 * Caffeine generates a class for each kind of cache and cache entry, such as {@code SSLA} or {@code PSW}, and
 * picks one of them by name from the configuration of the cache. It then reads the static {@code FACTORY} field
 * of the class with a {@code VarHandle} or looks up its constructor, and the class looks up its own fields with
 * {@code VarHandle}s. Since the configuration is only known at runtime, every generated class of Caffeine is
 * registered.
 *
 * @author Tim Yates
 * @since 4.0.0
 */
@Internal
public class CaffeineFeature implements Feature {

    @Override
    public String getDescription() {
        return "Registers the classes that Caffeine generates for each kind of cache and cache entry";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerFields(access, "com.github.benmanes.caffeine.cache.BLCHeader$DrainStatusRef", "drainStatus");
        registerFields(access, "com.github.benmanes.caffeine.cache.BBHeader$ReadCounterRef", "readCounter");
        registerFields(access, "com.github.benmanes.caffeine.cache.BBHeader$ReadAndWriteCounterRef", "writeCounter");
        registerFields(access, "com.github.benmanes.caffeine.cache.StripedBuffer", "tableBusy");
        registerFields(access, "java.lang.Thread", "threadLocalRandomProbe");

        Class<?> localCacheFactory = access.findClassByName(CaffeineGeneratedClasses.LOCAL_CACHE_FACTORY);
        if (localCacheFactory != null) {
            for (String className : CaffeineGeneratedClasses.classNames(localCacheFactory)) {
                Class<?> type = access.findClassByName(className);
                if (type != null) {
                    registerGeneratedClass(type);
                }
            }
        }
    }

    private void registerGeneratedClass(Class<?> type) {
        RuntimeReflection.register(type);
        RuntimeReflection.register(type.getDeclaredConstructors());
        RuntimeReflection.register(type.getDeclaredFields());
    }

    private void registerFields(BeforeAnalysisAccess access, String clz, String... fields) {
        Class<?> type = access.findClassByName(clz);
        if (type == null) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            if (Arrays.asList(fields).contains(field.getName())) {
                RuntimeReflection.register(field);
            }
        }
    }
}
