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

import io.micronaut.cache.oracle.persistence.CacheEntryEntity
import io.micronaut.cache.oracle.persistence.CacheEntryId
import io.micronaut.cache.oracle.persistence.OracleCacheEntryRepository
import io.micronaut.context.ApplicationContext

import java.time.Instant

class OracleCleanupScheduleIntegrationTest extends OracleIntegrationSupport {

    void cleanupSchedulerRegistersJobAndDeletesExpiredEntries() {
        given:
        ApplicationContext context = newContext([
            'micronaut.caches.orders.cleanup-interval': '1s',
        ])
        OracleCacheEntryRepository repository = context.getBean(OracleCacheEntryRepository)
        repository.invalidateCache('orders')
        repository.save(expiredEntry())

        expect:
        cleanupJobIsRegistered('MN_CACHE_CLEANUP_ORDERS', 'FREQ=SECONDLY;INTERVAL=1')

        when:
        boolean cleanupRan = waitForCondition(20_000L, 200L) {
            schedulerRunCount('MN_CACHE_CLEANUP_ORDERS') > 0L
        }
        boolean expiredEntryDeleted = waitForCondition(20_000L, 200L) {
            repository.countByIdCacheName('orders') == 0L
        }

        then:
        cleanupRan
        expiredEntryDeleted

        cleanup:
        context.close()
    }

    private boolean cleanupJobIsRegistered(String jobName, String expectedInterval) {
        try (def connection = openJdbcConnection();
             def statement = connection.prepareStatement('''
                 SELECT ENABLED, REPEAT_INTERVAL
                 FROM USER_SCHEDULER_JOBS
                 WHERE JOB_NAME = ?
             ''')) {
            statement.setString(1, jobName)
            try (def rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false
                }
                return 'TRUE'.equals(rs.getString('ENABLED')) && rs.getString('REPEAT_INTERVAL') == expectedInterval
            }
        }
    }

    private long schedulerRunCount(String jobName) {
        try (def connection = openJdbcConnection();
             def statement = connection.prepareStatement('''
                 SELECT RUN_COUNT
                 FROM USER_SCHEDULER_JOBS
                 WHERE JOB_NAME = ?
             ''')) {
            statement.setString(1, jobName)
            try (def rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return 0L
                }
                return rs.getLong('RUN_COUNT')
            }
        }
    }

    private static boolean waitForCondition(long timeoutMillis, long pollIntervalMillis, Closure<Boolean> condition) {
        long deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) {
                return true
            }
            Thread.sleep(pollIntervalMillis)
        }
        return condition.call()
    }

    private static CacheEntryEntity expiredEntry() {
        return new CacheEntryEntity(
            new CacheEntryId('orders', [7, 7, 7] as byte[]),
            [8, 8, 8] as byte[],
            'value'.bytes,
            1L,
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(30)
        )
    }
}
