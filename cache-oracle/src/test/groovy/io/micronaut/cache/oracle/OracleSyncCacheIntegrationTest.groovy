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
package io.micronaut.cache.oracle

import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.inject.qualifiers.Qualifiers

import java.util.concurrent.atomic.AtomicInteger

class OracleSyncCacheIntegrationTest extends OracleIntegrationSupport {

    void putGetAndInvalidateUseRealOracleTable() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        cache.put('car:model:s3', 101)
        Optional<Integer> stored = cache.get('car:model:s3', Argument.of(Integer))
        cache.invalidate('car:model:s3')
        Optional<Integer> afterInvalidate = cache.get('car:model:s3', Argument.of(Integer))

        then:
        stored.present
        stored.get() == 101
        afterInvalidate.empty

        cleanup:
        context.close()
    }

    void putIfAbsentReturnsExistingValueFromDatabase() {
        given:
        ApplicationContext context = newContext()
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))

        when:
        cache.put('car:model:x', 7)
        Optional<Integer> duplicate = cache.putIfAbsent('car:model:x', 9)

        then:
        duplicate.present
        duplicate.get() == 7

        cleanup:
        context.close()
    }

    void blockingModeReadsPersistedValueWithoutRecomputing() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.blocking': true,
        ])
        OracleSyncCache cache = context.getBean(OracleSyncCache, Qualifiers.byName('orders'))
        AtomicInteger supplierCalls = new AtomicInteger(0)

        when:
        Integer first = cache.get('car:model:blocking', Argument.of(Integer), {
            supplierCalls.incrementAndGet()
            return 42
        })
        Integer second = cache.get('car:model:blocking', Argument.of(Integer), {
            supplierCalls.incrementAndGet()
            return 84
        })

        then:
        first == 42
        second == 42
        supplierCalls.get() == 1

        cleanup:
        context.close()
    }
}
