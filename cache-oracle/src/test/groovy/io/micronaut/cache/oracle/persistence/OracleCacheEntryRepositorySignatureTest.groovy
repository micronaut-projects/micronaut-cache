package io.micronaut.cache.oracle.persistence

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import spock.lang.Specification

import java.time.Instant

class OracleCacheEntryRepositorySignatureTest extends Specification {

    void nullableRepositoryParametersAreDeclaredForOptionalValues() {
        when:
        def blockingPut = OracleCacheEntryRepository.getMethod(
            'blockingPut',
            String,
            byte[],
            byte[],
            byte[],
            Instant,
            Long.TYPE
        )
        def updateLastAccess = OracleCacheEntryRepository.getMethod(
            'updateLastAccess',
            String,
            byte[],
            Instant,
            Instant
        )

        then:
        // Nullable contract is important because expiry can be intentionally omitted in both APIs.
        blockingPut.parameters[4].isAnnotationPresent(Nullable)
        updateLastAccess.parameters[3].isAnnotationPresent(Nullable)
    }

    void finderAndCountMethodsUseExplicitNativeQueries() {
        when:
        def findByKey = OracleCacheEntryRepository.getMethod(
            'findByIdCacheNameAndIdKeyHash',
            String,
            byte[],
        )
        def countByCache = OracleCacheEntryRepository.getMethod(
            'countByIdCacheName',
            String,
        )

        Query findByKeyQuery = findByKey.getAnnotation(Query)
        Query countByCacheQuery = countByCache.getAnnotation(Query)

        then:
        // We pin explicit native SQL because Oracle LOB comparison behavior is easy to regress.
        findByKeyQuery != null
        findByKeyQuery.nativeQuery()
        findByKeyQuery.value().contains('KEY_HASH = :keyHash')
        findByKeyQuery.value().contains('WHERE CACHE_NAME = :cacheName AND KEY_HASH = :keyHash')
        !findByKeyQuery.value().contains('DBMS_LOB.COMPARE')
        countByCacheQuery != null
        countByCacheQuery.nativeQuery()
    }

    void updateAndInvalidateQueriesUseHashOnlyPredicates() {
        when:
        def updateLastAccess = OracleCacheEntryRepository.getMethod(
            'updateLastAccess',
            String,
            byte[],
            Instant,
            Instant,
        )
        def invalidateKey = OracleCacheEntryRepository.getMethod(
            'invalidateKey',
            String,
            byte[],
        )

        Query updateLastAccessQuery = updateLastAccess.getAnnotation(Query)
        Query invalidateKeyQuery = invalidateKey.getAnnotation(Query)

        then:
        // Hash-only predicates preserve index usage and avoid payload-level comparisons.
        updateLastAccessQuery.value().contains('KEY_HASH = :keyHash')
        !updateLastAccessQuery.value().contains('KEY_PAYLOAD')
        invalidateKeyQuery.value().contains('KEY_HASH = :keyHash')
        !invalidateKeyQuery.value().contains('KEY_PAYLOAD')
    }

    void totalWeightTreatsNullWeightsAsOne() {
        when:
        def totalWeight = OracleCacheEntryRepository.getMethod(
            'totalWeight',
            String,
        )

        Query totalWeightQuery = totalWeight.getAnnotation(Query)

        then:
        totalWeightQuery != null
        totalWeightQuery.nativeQuery()
        totalWeightQuery.value().contains('SUM(COALESCE(VALUE_WEIGHT, 1))')
    }
}
