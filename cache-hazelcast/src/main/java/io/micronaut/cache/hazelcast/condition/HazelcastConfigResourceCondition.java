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
package io.micronaut.cache.hazelcast.condition;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.file.FileSystemResourceLoader;
import io.micronaut.core.io.scan.ClassPathResourceLoader;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Condition classes for checking Hazelcast config resources.
 *
 * @since 1.0.0
 */
public class HazelcastConfigResourceCondition implements Condition {

    public static final String[] CLIENT_CONFIG_FILES = {"hazelcast-client.xml", "hazelcast-client.yml",
                                                        "classpath:hazelcast-client.xml", "classpath:hazelcast-client.yml"};
    public static final String[] INSTANCE_CONFIG_FILES = {"hazelcast.xml", "hazelcast.yml", "classpath:hazelcast.xml",
                                                          "classpath:hazelcast.yml"};

    @Override
    public boolean matches(ConditionContext context) {
        String[] allConfigFiles = Stream.concat(Arrays.stream(CLIENT_CONFIG_FILES), Arrays.stream(INSTANCE_CONFIG_FILES))
                .toArray(String[]::new);
        return !resourceExists(context, allConfigFiles);
    }

    /**
     * Checks whether any path given exists.
     *
     * @param context the condition context
     * @param paths the paths to check
     * @return true if any of the given paths exists. False otherwise.
     */
    protected boolean resourceExists(ConditionContext<?> context, String[] paths) {
        final BeanContext beanContext = context.getBeanContext();
        ResourceResolver resolver;
        final List<ResourceLoader> resourceLoaders;
        if (beanContext instanceof ApplicationContext) {
            ResourceLoader resourceLoader = ((ApplicationContext) beanContext).getEnvironment();
            resourceLoaders = Arrays.asList(resourceLoader, FileSystemResourceLoader.defaultLoader());
        } else {
            resourceLoaders = Arrays.asList(
                    ClassPathResourceLoader.defaultLoader(beanContext.getClassLoader()),
                    FileSystemResourceLoader.defaultLoader()
            );
        }
        resolver = new ResourceResolver(resourceLoaders);
        for (String resourcePath : paths) {
            if (resolver.getResource(resourcePath).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Condition that matches if any client config file exists.
     */
    public static class HazelcastClientConfigCondition extends HazelcastConfigResourceCondition {
        @Override
        public boolean matches(ConditionContext context) {
            return resourceExists(context, CLIENT_CONFIG_FILES);
        }
    }

    /**
     * Condition that matches if any instance config file exists.
     */
    public static class HazelcastInstanceConfigCondition extends HazelcastConfigResourceCondition {
        @Override
        public boolean matches(ConditionContext context) {
            return resourceExists(context, INSTANCE_CONFIG_FILES);
        }
    }

}
