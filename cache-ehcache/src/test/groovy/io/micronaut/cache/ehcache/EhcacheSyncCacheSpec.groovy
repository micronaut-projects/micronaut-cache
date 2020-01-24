package io.micronaut.cache.ehcache

import io.micronaut.cache.SyncCache
import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext

class EhcacheSyncCacheSpec extends AbstractSyncCacheSpec {

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "ehcache.caches.counter.enabled": true,
                "ehcache.caches.counter2.enabled": true
        )
    }

}
