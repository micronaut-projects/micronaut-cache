package io.micronaut.cache

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "spec.name", value = "ConditionalCacheSpec")
class ConditionalCacheSpec extends Specification {

    @Inject
    ConditionalService service

    void "test conditional cache"() {
        when:
        def results = (1..10).collect { service.get(it) }
        sleep(10)
        def secondResults = (1..10).collect { service.get(it) }

        then: "condition results in the last 5 results being cached"
        results.drop(5) == secondResults.drop(5)

        and: "the first 5 results are not cached"
        results.take(5).every {!secondResults.contains(it) }
    }
}
