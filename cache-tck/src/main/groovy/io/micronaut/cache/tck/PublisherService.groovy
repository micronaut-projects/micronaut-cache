/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.tck

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.async.annotation.SingleResult
import io.reactivex.Flowable
import io.reactivex.Single

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger

@Singleton
@CacheConfig('counter')
class PublisherService {

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