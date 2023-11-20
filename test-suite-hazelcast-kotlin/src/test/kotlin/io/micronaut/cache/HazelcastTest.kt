package io.micronaut.cache

import io.micronaut.cache.hazelcast.HazelcastCacheManager
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@MicronautTest
@Testcontainers
internal class HazelcastTest : TestPropertyProvider {

    @Container
    val hazelcast: GenericContainer<*> =
        GenericContainer("hazelcast/hazelcast:" + System.getProperty("hazelcastVersion"))
            .withExposedPorts(5701)

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var cacheManager: HazelcastCacheManager

    @Test
    fun simpleTest() {
        val client = httpClient.toBlocking()

        var inc = client.retrieve("/inc", Int::class.java)
        assertEquals(1, inc)

        inc = client.retrieve("/inc", Int::class.java)
        assertEquals(2, inc)

        val get = client.retrieve("/get", Int::class.java)
        assertEquals(2, get)
        assertEquals(2, cacheManager.getCache("counter").get("test", Int::class.java).get())

        client.exchange<Any>("/del")

        Assertions.assertTrue(cacheManager.getCache("counter").get("test", Int::class.java).isEmpty)
    }

    override fun getProperties() = mapOf(
        "hazelcast.client.network.addresses" to "${hazelcast.host}:${hazelcast.firstMappedPort}"
    )
}
