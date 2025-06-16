package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Singleton
@CacheConfig(cacheNames = ["suspendedDefault"])
open class DefaultSuspendService {

    open var calls: Int = 0

    @Cacheable
    open suspend fun suspended(first: Int): Int {
        calls++
        return first
    }
}

@Singleton
@CacheConfig(cacheNames = ["suspended"])
open class ExplicitSuspendService {

    open var calls: Int = 0

    @Cacheable(parameters = ["first"])
    open suspend fun suspended(first: Int): Int {
        calls++
        return first
    }
}

@MicronautTest
internal class SuspendCacheTests {

    @Inject
    lateinit var defaultService: DefaultSuspendService

    @Inject
    lateinit var explicitService: ExplicitSuspendService

    @Test
    fun `caches all parameters for suspend function when none explicitly declared`() = runBlocking {
        defaultService.calls = 0

        // first call -> executes
        assertEquals(1, defaultService.suspended(1))
        assertEquals(1, defaultService.calls)

        // same arg -> cached
        assertEquals(1, defaultService.suspended(1))
        assertEquals(1, defaultService.calls)

        // new arg -> executes again
        assertEquals(2, defaultService.suspended(2))
        assertEquals(2, defaultService.calls)

        // cached for second arg
        assertEquals(2, defaultService.suspended(2))
        assertEquals(2, defaultService.calls)
    }

    @Test
    fun `caches only explicit parameters for suspend function`() = runBlocking {
        explicitService.calls = 0

        // first call with 1 -> executes
        assertEquals(1, explicitService.suspended(1))
        assertEquals(1, explicitService.calls)

        // same arg -> cached
        assertEquals(1, explicitService.suspended(1))
        assertEquals(1, explicitService.calls)

        // new arg 2 -> executes again
        assertEquals(2, explicitService.suspended(2))
        assertEquals(2, explicitService.calls)

        // cached for second arg
        assertEquals(2, explicitService.suspended(2))
        assertEquals(2, explicitService.calls)
    }
}
