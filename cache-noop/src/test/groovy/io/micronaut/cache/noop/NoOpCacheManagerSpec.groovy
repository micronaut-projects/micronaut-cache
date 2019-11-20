/*
 * Copyright 2017-2019 original authors
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

import io.micronaut.cache.SyncCache
import io.micronaut.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @since 1.0.0
 */
class NoOpCacheManagerSpec extends Specification {

    @Shared
    NoOpCacheManager cacheManager

    def setupSpec() {
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "noop-cache.enabled": true
        ])
        this.cacheManager = ctx.getBean(NoOpCacheManager)
    }

    void "test get cache"() {
        setup:
        String name = createRandomKey()

        when:
        SyncCache syncCache = this.cacheManager.getCache(name)

        then:
        syncCache != null
        syncCache instanceof NoOpSyncCache
        syncCache.is(this.cacheManager.getCache(name))
    }

    void "test cache names"() {
        setup:
        String name = createRandomKey()

        expect:
        this.cacheManager.cacheNames.contains(name) == false

        when:
        this.cacheManager.getCache(name)

        then:
        this.cacheManager.cacheNames.contains(name) == true
    }

    void "test no op cache"() {
        setup:
        String name = createRandomKey()

        when:
        SyncCache syncCache = this.cacheManager.getCache(name)

        then:
        syncCache.name == name

        when:
        Object key = new Object()
        syncCache.put(key, new Object())

        then:
        syncCache.get(key, Object).isPresent() == false
        syncCache.getNativeCache().is(syncCache)
    }

    void "test cache callable"() {
        setup:
        String name = createRandomKey()

        when:
        SyncCache syncCache = this.cacheManager.getCache(name)
        Object key = new Object()
        Object returnValue = new Object()
        Object value = syncCache.get(key, Object, { -> returnValue })

        then:
        value == returnValue
    }

    void "test cache callable fail"() {
        setup:
        String name = createRandomKey()

        when:
        SyncCache syncCache = this.cacheManager.getCache(name)
        Object key = new Object()
        Object value = syncCache.get(key, Object, { -> throw new UnsupportedOperationException("Expected exception") })

        then:
        UnsupportedOperationException ex = thrown()
        ex.message == "Expected exception"
    }

    private String createRandomKey() {
        return UUID.randomUUID().toString()
    }
}
