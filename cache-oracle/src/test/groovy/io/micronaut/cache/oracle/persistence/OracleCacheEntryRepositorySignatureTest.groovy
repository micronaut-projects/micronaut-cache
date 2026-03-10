package io.micronaut.cache.oracle.persistence

import io.micronaut.core.annotation.Nullable
import spock.lang.Specification

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
            byte[],
            Instant,
            Instant
        )

        then:
        blockingPut.parameters[4].isAnnotationPresent(Nullable)
        updateLastAccess.parameters[4].isAnnotationPresent(Nullable)
    }
}
