package io.micronaut.cache

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
@CacheConfig(cacheNames = ["conditional"])
open class ConditionalService {

    private val repository = DocRepo()

    // tag::conditional[]
    data class Id(val value: Int)

    @Cacheable(condition = "#{id.value > 5}")
    open fun get(id: Id) = repository[id]
    // end::conditional[]

    // here to make the docs look nice when we extract the function above
    private class DocRepo {
        operator fun get(id: Id): String {
            return "test " + id.value + " " + System.currentTimeMillis()
        }
    }
}
