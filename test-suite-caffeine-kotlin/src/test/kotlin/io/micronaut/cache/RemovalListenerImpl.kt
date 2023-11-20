package io.micronaut.cache

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import jakarta.inject.Singleton

// tag::clazz[]
@Singleton
class RemovalListenerImpl internal constructor(private val handler: MyRemovalHandler) : RemovalListener<String?, Int?> {

    override fun onRemoval(key: String?, value: Int?, cause: RemovalCause) {
        handler.handle(key!!, value!!, cause)
    }
}
// end::clazz[]
