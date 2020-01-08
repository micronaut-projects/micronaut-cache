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

    public static final String[] CLIENT_CONFIG_FILES = {"hazelcast-client.xml", "hazelcast-client.yml"};
    public static final String[] INSTANCE_CONFIG_FILES = {"hazelcast.xml", "hazelcast.yml"};

    @Override
    public boolean matches(ConditionContext context) {
        String[] allConfigFiles = Stream.concat(Arrays.stream(CLIENT_CONFIG_FILES), Arrays.stream(INSTANCE_CONFIG_FILES))
                .toArray(String[]::new);
        return !resourceExists(context, allConfigFiles);
    }

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

    public static class HazelcastClientConfigCondition extends HazelcastConfigResourceCondition {
        @Override
        public boolean matches(ConditionContext context) {
            return resourceExists(context, CLIENT_CONFIG_FILES);
        }
    }

    public static class HazelcastInstanceConfigCondition extends HazelcastConfigResourceCondition {
        @Override
        public boolean matches(ConditionContext context) {
            return resourceExists(context, INSTANCE_CONFIG_FILES);
        }
    }

}
