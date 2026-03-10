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

import io.micronaut.cache.oracle.configuration.OracleCacheConfiguration
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

class OracleCacheConfigurationTest extends Specification {

    void multipleConfigurationsCoexist() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.orders.cleanup-interval' : '30s',
                'micronaut.caches.orders.lock-wait-timeout': '2s',
                'micronaut.caches.users.cleanup-interval'  : '45s',
                'micronaut.caches.users.lock-wait-timeout' : '3s',
                'micronaut.caches.users.blocking'          : true
        ])

        when:
        OracleCacheConfiguration orders = context.getBean(OracleCacheConfiguration, Qualifiers.byName('orders'))
        OracleCacheConfiguration users = context.getBean(OracleCacheConfiguration, Qualifiers.byName('users'))

        then:
        orders.cacheName == 'orders'
        users.cacheName == 'users'
        !orders.blocking
        users.blocking
        orders.cleanupInterval.seconds == 30
        users.cleanupInterval.seconds == 45
        orders.lockWaitTimeout.seconds == 2
        users.lockWaitTimeout.seconds == 3

        cleanup:
        context.close()
    }

    void configurationRowsMatchCacheConfiguration() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.orders.blocking'                : true,
                'micronaut.caches.orders.lock-wait-timeout'       : '15s',
                'micronaut.caches.orders.cleanup-interval'        : '30s',
                'micronaut.caches.orders.expire-after-write'      : '5m',
                'micronaut.caches.orders.expire-after-access'     : '2m',
                'micronaut.caches.orders.maximum-size'            : 100,
                'micronaut.caches.orders.maximum-weight'          : 200,
                'micronaut.caches.orders.record-stats'            : true,
                'micronaut.caches.orders.test-mode'               : true,
                'micronaut.caches.default.cleanup-interval'       : '1m',
                'micronaut.caches.default.lock-wait-timeout'      : '5s'
        ])

        when:
        OracleCacheConfiguration configuration = context.getBean(OracleCacheConfiguration, Qualifiers.byName('orders'))
        OracleCacheConfiguration defaultConfiguration = context.getBean(OracleCacheConfiguration, Qualifiers.byName('default'))

        then:
        configuration.cacheName == 'orders'
        configuration.blocking
        configuration.lockWaitTimeout.seconds == 15
        configuration.cleanupInterval.seconds == 30
        configuration.expireAfterWrite.get().toMinutes() == 5
        configuration.expireAfterAccess.get().toMinutes() == 2
        configuration.maximumSize.getAsLong() == 100
        configuration.maximumWeight.getAsLong() == 200
        configuration.recordStats
        configuration.testMode

        and:
        !defaultConfiguration.blocking
        defaultConfiguration.cleanupInterval.seconds == 60
        defaultConfiguration.lockWaitTimeout.seconds == 5

        cleanup:
        context.close()
    }

    void rejectsInvalidDurations() {
        when:
        ApplicationContext context = ApplicationContext.run([
                'micronaut.caches.bad.cleanup-interval'      : '-1s',
                'micronaut.caches.bad.lock-wait-timeout'     : '-5s',
                'micronaut.caches.bad.expire-after-write'    : '-1m',
                'micronaut.caches.bad.maximum-size'          : -1,
                'micronaut.caches.bad.maximum-weight'        : -2
        ])
        context.getBean(OracleCacheConfiguration, Qualifiers.byName('bad'))

        then:
        Exception ex = thrown()
        findIllegalArgumentException(ex) != null
    }

    private static IllegalArgumentException findIllegalArgumentException(Throwable throwable) {
        Throwable current = throwable
        while (current != null) {
            if (current instanceof IllegalArgumentException) {
                return (IllegalArgumentException) current
            }
            current = current.cause
        }
        null
    }
}
