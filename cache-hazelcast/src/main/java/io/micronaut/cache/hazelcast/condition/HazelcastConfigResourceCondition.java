package io.micronaut.cache.hazelcast.condition;

import io.micronaut.context.BeanContext;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.io.file.DefaultFileSystemResourceLoader;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;

import java.net.URL;
import java.util.Optional;

/**
 * Condition class for checking Hazelcast config resources.
 *
 * @since 1.0.0
 */
public class HazelcastConfigResourceCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        return !resourceExists("hazelcast.xml", "hazelcast.yml", "hazelcast-client.xml", "hazelcast-client.yml");
    }

    public static boolean resourceExists(String... paths){
        DefaultFileSystemResourceLoader fileSystemResourceLoader = new DefaultFileSystemResourceLoader(".");
        DefaultClassPathResourceLoader classPathResourceLoader = new DefaultClassPathResourceLoader(
                BeanContext.class.getClassLoader());
        for (String path : paths) {
            Optional<URL> fileConfig = fileSystemResourceLoader.getResource(path);
            if(fileConfig.isPresent()){
                return true;
            }
            Optional<URL> classpathConfig = classPathResourceLoader.getResource(path);
            if (classpathConfig.isPresent()){
                return true;
            }
        }
        return false;
    }
}
