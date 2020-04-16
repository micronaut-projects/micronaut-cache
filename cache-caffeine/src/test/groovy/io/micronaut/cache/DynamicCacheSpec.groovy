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
package io.micronaut.cache

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import spock.lang.Specification

import javax.inject.Singleton

class DynamicCacheSpec extends Specification {

    void "test behavior in the presence of a dynamic cache manager"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'spec.name': DynamicCacheSpec.simpleName,
                'micronaut.caches.counter.initialCapacity':10,
                'micronaut.caches.counter.testMode':true,
                'micronaut.caches.counter.maximumSize':20,
                'micronaut.caches.counter2.initialCapacity':10,
                'micronaut.caches.counter2.maximumSize':20,
                'micronaut.caches.counter2.testMode':true
        )
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        expect:
        cacheManager.cacheNames == ["counter", "counter2"] as Set

        when:
        SyncCache cache = cacheManager.getCache('counter')

        then:
        noExceptionThrown()
        cache != null

        when:
        cache = cacheManager.getCache("fooBar")

        then:
        noExceptionThrown()
        cache instanceof DynamicCache
        cacheManager.cacheNames == ["counter", "counter2", "fooBar"] as Set

        cleanup:
        applicationContext.close()
    }

    @Requires(property = "spec.name", value = "DynamicCacheSpec")
    @Singleton
    static class MyDynamicCacheManager implements DynamicCacheManager<Map> {

        @Override
        SyncCache<Map> getCache(String name) {
            return new DynamicCache()
        }
    }

}
