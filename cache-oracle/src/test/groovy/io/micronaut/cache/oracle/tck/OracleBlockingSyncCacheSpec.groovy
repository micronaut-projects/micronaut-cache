/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.cache.oracle

import io.micronaut.cache.tck.AbstractSyncCacheSpec
import io.micronaut.context.ApplicationContext

class OracleBlockingSyncCacheSpec extends AbstractSyncCacheSpec {

    void "test cacheable annotations"() {
        super."test cacheable annotations"()
    }

    @Override
    ApplicationContext createApplicationContext() {
        return OracleTckSupport.sharedContext([
            'micronaut.caches.counter.enabled' : true,
            'micronaut.caches.counter2.enabled': true,
            'micronaut.caches.test.enabled'    : true,
            'micronaut.caches.counter.blocking' : true,
            'micronaut.caches.counter2.blocking': true,
            'micronaut.caches.test.blocking'    : true,
            'micronaut.caches.counter.cleanup-interval' : '1h',
            'micronaut.caches.counter2.cleanup-interval': '1h',
            'micronaut.caches.test.cleanup-interval'    : '1h',
        ])
    }
}
