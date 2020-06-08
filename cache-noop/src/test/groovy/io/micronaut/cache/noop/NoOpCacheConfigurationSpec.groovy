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
package io.micronaut.cache.noop

import io.micronaut.cache.CacheManager
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @since 1.0.0
 */
class NoOpCacheConfigurationSpec extends Specification {

    void "test no operation cache manager is not created when noop-cache.enabled property is not defined"() {
        setup:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext)

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        !(cacheManager instanceof NoOpCacheManager)
    }

    void "test no operation cache manager is not created when noop-cache.enabled = false"() {
        setup:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "noop-cache.enabled": false
        ])

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        !(cacheManager instanceof NoOpCacheManager)
    }

    void "test no operation cache manager is created when noop-cache.enabled = true"() {
        setup:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "noop-cache.enabled": true
        ])

        when:
        CacheManager cacheManager = ctx.getBean(CacheManager)

        then:
        cacheManager instanceof NoOpCacheManager
    }
}
