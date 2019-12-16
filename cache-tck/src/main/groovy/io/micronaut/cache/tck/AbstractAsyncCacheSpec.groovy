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
package io.micronaut.cache.tck

import io.micronaut.cache.AsyncCache
import io.micronaut.cache.CacheManager
import io.micronaut.context.ApplicationContext
import spock.lang.Retry
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Supplier

@Retry
abstract class AbstractAsyncCacheSpec extends Specification {

    abstract ApplicationContext createApplicationContext()

    abstract void flushCache(AsyncCache syncCache)

    void "test async cacheable annotations"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        PollingConditions conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when:
        AsyncCounterService counterService = applicationContext.getBean(AsyncCounterService)

        then:
        counterService.flowableValue("test").blockingFirst() == 0
        counterService.singleValue("test").blockingGet() == 0

        when:
        counterService.reset()
        def result =counterService.increment("test")

        then:
        conditions.eventually {
            result == 1
            counterService.flowableValue("test").blockingFirst() == 1
            counterService.futureValue("test").get() == 1
            counterService.stageValue("test").toCompletableFuture().get() == 1
            counterService.singleValue("test").blockingGet() == 1
            counterService.getValue("test") == 1
        }

        when:
        result = counterService.incrementNoCache("test")

        then:
        result == 2
        counterService.flowableValue("test").blockingFirst() == 1
        counterService.futureValue("test").get() == 1
        counterService.stageValue("test").toCompletableFuture().get() == 1
        counterService.singleValue("test").blockingGet() == 1
        counterService.getValue("test") == 1

        when:
        counterService.reset("test")

        then:
        conditions.eventually {
            counterService.getValue("test") == 0
        }

        when:
        counterService.reset("test")

        then:
        conditions.eventually {
            counterService.futureValue("test").get() == 0
            counterService.stageValue("test").toCompletableFuture().get() == 0
        }

        when:
        counterService.set("test", 3)

        then:
        conditions.eventually {
            counterService.getValue("test") == 3
            counterService.futureValue("test").get() == 3
            counterService.stageValue("test").toCompletableFuture().get() == 3
        }

        when:
        result = counterService.increment("test")

        then:
        conditions.eventually {
            result == 4
            counterService.getValue("test") == 4
            counterService.futureValue("test").get() == 4
            counterService.stageValue("test").toCompletableFuture().get() == 4
        }

        when:
        result = counterService.futureIncrement("test").get()

        then:
        conditions.eventually {
            result == 5
            counterService.getValue("test") == 5
            counterService.futureValue("test").get() == 5
            counterService.stageValue("test").toCompletableFuture().get() == 5
        }

        when:
        counterService.reset()

        then:
        conditions.eventually {
            !counterService.getOptionalValue("test").isPresent()
            counterService.getValue("test") == 0
            counterService.getOptionalValue("test").isPresent()
            counterService.getValue2("test") == 0
        }

        when:
        counterService.increment("test")
        counterService.increment("test")

        then:
        conditions.eventually {
            counterService.getValue("test") == 2
            counterService.getValue2("test") == 0
        }

        when:
        counterService.increment2("test")

        then:
        conditions.eventually {
            counterService.getValue("test") == 1
            counterService.getValue2("test") == 1
        }
    }

    void "test configure async cache"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        when:
        AsyncCache asyncCache = applicationContext.get("test", AsyncCache).orElse(cacheManager.getCache('test').async())

        then:
        asyncCache.name == 'test'

        when:
        asyncCache.put("one", 1)
        asyncCache.put("two", 2)
        asyncCache.put("three", 3)

        asyncCache.get("two", Integer)
        asyncCache.get("three", Integer)

        asyncCache.put("four", 4)
        flushCache(asyncCache)
        PollingConditions conditions = new PollingConditions(timeout: 15, delay: 0.5)

        then:
        conditions.eventually {
            !asyncCache.get("one", Integer).get().isPresent()
            asyncCache.get("two", Integer).get().isPresent()
            asyncCache.get("three", Integer).get().isPresent()
            asyncCache.get("four", Integer).get().isPresent()
        }

        when:
        asyncCache.invalidate("two")

        then:
        conditions.eventually {
            !asyncCache.get("one", Integer).get().isPresent()
            !asyncCache.get("two", Integer).get().isPresent()
            asyncCache.get("three", Integer).get().isPresent()
            asyncCache.putIfAbsent("three", 3).get().isPresent()
            asyncCache.get("four", Integer).get().isPresent()
        }

        when:
        asyncCache.invalidateAll()

        then:
        conditions.eventually {
            !asyncCache.get("one", Integer).get().isPresent()
            !asyncCache.get("two", Integer).get().isPresent()
            !asyncCache.get("three", Integer).get().isPresent()
            !asyncCache.get("four", Integer).get().isPresent()
        }

        cleanup:
        applicationContext.stop()
    }

    void "test get with supplier"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        when:
        AsyncCache asyncCache = applicationContext.get("test", AsyncCache).orElse(cacheManager.getCache('test').async())

        asyncCache.get("five", Integer, new Supplier<Integer>() {
            Integer get() {
                return 5
            }
        })
        PollingConditions conditions = new PollingConditions(timeout: 15, delay: 0.5)

        then:
        conditions.eventually {
            asyncCache.get("five", Integer).get().isPresent()
        }

        cleanup:
        applicationContext.stop()
    }

    void "test async cache putIfAbsent"() {
        given:
        ApplicationContext applicationContext = createApplicationContext()
        CacheManager cacheManager = applicationContext.getBean(CacheManager)

        when:
        AsyncCache asyncCache = applicationContext.get("test", AsyncCache).orElse(cacheManager.getCache('test').async())
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
