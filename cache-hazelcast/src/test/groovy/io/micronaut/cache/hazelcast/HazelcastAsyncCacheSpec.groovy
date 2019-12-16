package io.micronaut.cache.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.AsyncCache
import io.micronaut.cache.CacheManager
import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions


class HazelcastAsyncCacheSpec extends Specification  {
    @Shared
    HazelcastInstance hazelcastServerInstance

    def setupSpec() {
        Config config = new Config("sampleCache")
        hazelcastServerInstance = Hazelcast.getOrCreateHazelcastInstance(config)
    }

    def cleanupSpec() {
        hazelcastServerInstance.shutdown()
    }

    ApplicationContext createApplicationContext() {
        return ApplicationContext.run(
                "hazelcast.instanceName": "sampleCache",
                "hazelcast.network.addresses": ['127.0.0.1:5701']
        )
    }

    void "test async cache putIfAbsent"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        when:
        SyncCache syncCache = applicationContext.get("test", SyncCache).orElse(cacheManager.getCache('test'))
        AsyncCache asyncCache = syncCache.async()
        PollingConditions conditions = new PollingConditions(timeout: 15, delay: 0.5)
        asyncCache.invalidate("one")

        then:
        conditions.eventually {
            !asyncCache.putIfAbsent("one", 1).get().isPresent()
        }

        when:
        asyncCache.put("two", 2)

        then:
        conditions.eventually {
            asyncCache.putIfAbsent("two", 2).get().isPresent()
        }

        cleanup:
        applicationContext.stop()
    }
}
