package io.micronaut.cache

import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import java.util.List
import java.util.stream.Stream

@MicronautTest
@Property(name = "micronaut.caches.counter.initial-capacity", value = "10")
@Property(name = "micronaut.caches.counter.test-mode", value = "true")
@Property(name = "micronaut.caches.counter.maximum-size", value = "20")
@Property(name = "micronaut.caches.counter.listen-to-removals", value = "true")
class CaffeineTest extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    @Inject
    BeanContext ctx

    def 'simple test'() {
        given:
        BlockingHttpClient client = httpClient.toBlocking()
        MyRemovalHandler bean = ctx.getBean(MyRemovalHandler.class)

        when:
        Integer inc = client.retrieve("/inc", Integer.class)

        then:
        inc == 1
        bean.removals.empty

        when:
        inc = client.retrieve("/inc", Integer.class)

        then:
        inc == 2
        bean.removals == ["test|1|REPLACED"]

        when:
        Integer get = client.retrieve("/get", Integer.class)

        then:
        get == 2

        when:
        client.exchange("/del")

        then:
        bean.removals == ["test|1|REPLACED", "test|2|EXPLICIT"]
    }

    def "conditional test"() {
        given:
        ConditionalService bean = ctx.getBean(ConditionalService)

        when:
        List<String> first = (1..10).collect { bean.get(new ConditionalService.Id(it)) }
        List<String> second = (1..10).collect { bean.get(new ConditionalService.Id(it)) }

        then:
        first.take(5) != second.take(5)
        first.drop(5) == second.drop(5)
    }
}
