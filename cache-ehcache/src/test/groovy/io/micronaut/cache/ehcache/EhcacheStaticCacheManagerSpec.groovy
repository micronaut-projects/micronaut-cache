package io.micronaut.cache.ehcache

import io.micronaut.cache.tck.AbstractStaticCacheManagerSpec
import io.micronaut.context.ApplicationContext

class EhcacheStaticCacheManagerSpec extends AbstractStaticCacheManagerSpec {

    @Override
    ApplicationContext createApplicationContext() {
        return ApplicationContext.run()
    }

}
