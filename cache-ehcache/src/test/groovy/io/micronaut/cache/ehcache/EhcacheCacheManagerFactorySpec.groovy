/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.ehcache

import io.micronaut.context.ApplicationContext
import org.ehcache.CacheManager
import org.ehcache.Status
import org.ehcache.core.internal.statistics.DefaultStatisticsService
import org.ehcache.core.spi.service.StatisticsService
import spock.lang.Specification

class EhcacheCacheManagerFactorySpec extends Specification {

    void "it creates a cache manager and initialises it"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(["ehcache.enabled": true])

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        cacheManager.status == Status.AVAILABLE

        cleanup:
        ctx.close()
    }

    void "it creates an statistics service"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(["ehcache.caches.foo.enabled": true])

        when:
        DefaultStatisticsService statisticsService = (DefaultStatisticsService) ctx.getBean(StatisticsService)
        def manager = ctx.getBean(io.micronaut.cache.CacheManager) //Triggering CacheManager initialisation
        manager.getCache("foo").put("cheese", "cheddar")

        then:
        statisticsService.getCacheStatistics("foo").cachePuts == 1

        cleanup:
        ctx.close()
    }
}
