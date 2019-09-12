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
package io.micronaut.cache.hazelcast

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.ApplicationContext
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single
import spock.lang.Retry
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Nirav Assar
 * @since 1.0
 */
@Retry
class HazelcastSyncCacheSpec extends Specification {

    @Shared
    HazelcastInstance hazelcastServerInstance

    def setupSpec() {
        hazelcastServerInstance = Hazelcast.newHazelcastInstance()
    }

    def cleanupSpec() {
        hazelcastServerInstance.shutdown()
    }

    void "test publisher cache methods are not called for hits"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                "hazelcast.instanceName": "sampleCache",
                "hazelcast.network-config.addresses": ['127.0.0.1:5701']
        )

        PublisherService publisherService = applicationContext.getBean(PublisherService)

        expect:
        publisherService.callCount.get() == 0

        when:
        publisherService.flowableValue("abc").blockingFirst()

        then:
        publisherService.callCount.get() == 1

        when:
        publisherService.flowableValue("abc").blockingFirst()

        then:
        publisherService.callCount.get() == 1

        when:
        publisherService.singleValue("abcd").blockingGet()

        then:
        publisherService.callCount.get() == 2

        when:
        publisherService.singleValue("abcd").blockingGet()

        then:
        publisherService.callCount.get() == 2
    }

    @Singleton
    @CacheConfig('counter')
    static class PublisherService {

        AtomicInteger callCount = new AtomicInteger()

        @Cacheable
        @SingleResult
        Flowable<Integer> flowableValue(String name) {
            callCount.incrementAndGet()
            return Flowable.just(0)
        }

        @Cacheable
        Single<Integer> singleValue(String name) {
            callCount.incrementAndGet()
            return Single.just(0)
        }

    }
}
