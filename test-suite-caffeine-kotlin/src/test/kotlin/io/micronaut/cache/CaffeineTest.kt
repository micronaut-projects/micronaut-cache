package io.micronaut.cache

import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest
@Property(name = "micronaut.caches.counter.initial-capacity", value = "10")
@Property(name = "micronaut.caches.counter.test-mode", value = "true")
@Property(name = "micronaut.caches.counter.maximum-size", value = "20")
@Property(name = "micronaut.caches.counter.listen-to-removals", value = "true")
internal class CaffeineTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var ctx: BeanContext

    @Test
    fun simpleTest() {
        val client = httpClient.toBlocking()
        val bean = ctx.getBean(MyRemovalHandler::class.java)

        var inc = client.retrieve("/inc", Int::class.java)
        assertEquals(1, inc)
        assertTrue { bean.removals.isEmpty() }

        inc = client.retrieve("/inc", Int::class.java)
        assertEquals(2, inc)
        val expectedEventsAfterReplacement = listOf("test|1|REPLACED")
        assertEquals(expectedEventsAfterReplacement, bean.removals)

        val get = client.retrieve("/get", Int::class.java)
        assertEquals(2, get)

        client.exchange<Any>("/del")
        val expectedEventsAfterRemoval = listOf("test|1|REPLACED", "test|2|EXPLICIT")
        assertEquals(expectedEventsAfterRemoval, bean.removals)
    }

    @Test
    fun conditionalTest() {
        val bean = ctx.getBean(ConditionalService::class.java)

        // Get the same thing twice, ids > 5 are cached
        val first = generateSequence(1) { it + 1 }.take(10).map { bean.get(ConditionalService.Id(it)) }.toList()
        val second = generateSequence(1) { it + 1 }.take(10).map { bean.get(ConditionalService.Id(it)) }.toList()

        Assertions.assertNotEquals(first.subList(0, 5), second.subList(0, 5))
        assertEquals(first.subList(5, 10), second.subList(5, 10))
    }
}
