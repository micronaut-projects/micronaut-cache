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
package io.micronaut.cache.interceptor;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.util.KotlinUtils;

import java.util.Arrays;

/**
 * <p>An implementation of the {@link CacheKeyGenerator} which works exactly like {@link DefaultCacheKeyGenerator} but drops the last parameter.
 * </p>
 *
 * @author Jacek Gajek
 * @since 3.2.3
 */
@Introspected
public class KotlinSuspendFunCacheKeyGenerator extends DefaultCacheKeyGenerator {

    @Override
    public Object generateKey(AnnotationMetadata metadata, Object... params) {
        String[] explicit = metadata.stringValues(Cacheable.class, "parameters");
        if (params == null || params.length == 0 || explicit.length > 0) {
            return super.generateKey(metadata, params);
        }

        int len = params.length;
        Object last = params[len - 1];

        boolean isContinuation = KotlinUtils.KOTLIN_COROUTINES_SUPPORTED
            && ClassUtils.forName("kotlin.coroutines.Continuation", getClass().getClassLoader())
            .map(contClass -> contClass.isInstance(last))
            .orElse(false);

        // drop hidden Continuation if present as last argument
        Object[] keyParams = isContinuation ? Arrays.copyOf(params, len - 1) : params;
        return super.generateKey(metadata, keyParams);
    }
}

