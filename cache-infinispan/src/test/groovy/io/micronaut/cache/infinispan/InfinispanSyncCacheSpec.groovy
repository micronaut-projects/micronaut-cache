package io.micronaut.cache.infinispan

import io.micronaut.cache.SyncCache
import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext

class InfinispanSyncCacheSpec extends AbstractSyncCacheSpec implements EmbeddedHotRodServerSupport {

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run("infinispan.client.hotrod.force-return-values": true)
    }

    @Override
    void flushCache(SyncCache syncCache) {
    }
}
