package io.micronaut.cache.docs

import com.github.benmanes.caffeine.cache.RemovalCause
import jakarta.inject.Singleton

@Singleton
class MyRemovalHandler {

    val removals: MutableList<String> = ArrayList()

    fun handle(key: String, value: Int, cause: RemovalCause) {
        removals.add("$key|$value|$cause")
    }
}
