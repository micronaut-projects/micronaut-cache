package io.micronaut.cache.ehcache

import io.micronaut.cache.AsyncCache
import io.micronaut.cache.tck.AbstractAsyncCacheSpec
import io.micronaut.context.ApplicationContext

class EhcacheAsyncCacheSpec extends AbstractAsyncCacheSpec {

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "ehcache.caches.counter.enabled": true,
                "ehcache.caches.counter2.enabled": true,
                "ehcache.caches.test.enabled": true
        )
    }

}
