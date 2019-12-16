package io.micronaut.cache.infinispan

import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext

class InfinispanAsyncCacheSpec extends AbstractAsyncCacheSpec implements EmbeddedHotRodServerSupport {

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run("infinispan.client.hotrod.force-return-values": true)
    }

    @Override
    void flushCache(AsyncCache syncCache) {
    }

}
