package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.infinispan.client.hotrod.RemoteCacheManager;

import javax.inject.Singleton;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Factory
public class InfinispanCacheFactory {

    private InfinispanHotRodClientConfiguration configuration;

    public InfinispanCacheFactory(InfinispanHotRodClientConfiguration configuration) {
        this.configuration = configuration;
    }

    @Singleton
    @Bean(preDestroy = "close")
    RemoteCacheManager remoteCacheManager() {
        return new RemoteCacheManager(configuration.getBuilder().build());
    }

}
