package io.micronaut.cache.infinispan

import io.micronaut.context.ApplicationContext
import org.infinispan.client.hotrod.RemoteCacheManager
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.manager.EmbeddedCacheManager
import spock.lang.Shared
import spock.lang.Specification

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
class InfinispanCacheFactorySpec extends Specification {

    @Shared
    EmbeddedCacheManager embeddedCacheManager

    void setupSpec() {
        embeddedCacheManager = new DefaultCacheManager()
    }

    void cleanupSpec() {
        embeddedCacheManager.stop()
    }

    void "it creates a remote cache manager"(){
        given:
        ApplicationContext ctx = ApplicationContext.run("infinispan.enabled": true)

        when:
        RemoteCacheManager remoteCacheManager = ctx.getBean(RemoteCacheManager)

        then:
        remoteCacheManager.started

        cleanup:
        ctx.close()
    }

}
