package io.micronaut.cache.oracle.persistence

import io.micronaut.core.annotation.Nullable
import spock.lang.Specification

import java.time.Instant

class OracleCacheEntryRepositorySignatureTest extends Specification {

    void nullableRepositoryParametersAreDeclaredForOptionalValues() {
        when:
        def upsert = OracleCacheEntryRepository.getMethod(
            'upsertCacheConfig',
            String,
            Integer.TYPE,
            Long.TYPE,
            Long.TYPE,
            Long,
            Long,
            Instant,
            Instant
        )
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
            byte[],
            Instant,
            Instant
        )

        then:
        upsert.parameters[4].isAnnotationPresent(Nullable)
        upsert.parameters[5].isAnnotationPresent(Nullable)
        blockingPut.parameters[4].isAnnotationPresent(Nullable)
        updateLastAccess.parameters[4].isAnnotationPresent(Nullable)
    }
}
