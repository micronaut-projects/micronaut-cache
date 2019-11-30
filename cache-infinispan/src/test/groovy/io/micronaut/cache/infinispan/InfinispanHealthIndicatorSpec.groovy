package io.micronaut.cache.infinispan

import io.micronaut.cache.infinispan.health.InfinispanClient
import io.micronaut.cache.infinispan.health.InfinispanHealthIndicator
import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import io.reactivex.Flowable
import org.infinispan.client.hotrod.RemoteCacheManager
import spock.lang.Specification


class InfinispanHealthIndicatorSpec extends Specification {

    void "it can get a health check"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                "infinispan.client.hotrod.server.host": "localhost",
                "infinispan.client.hotrod.server.port": 11222,
                "infinispan.client.hotrod.security.authentication.sasl-mechanism": "PLAIN",
                "infinispan.client.hotrod.security.authentication.username": "7H8jnWJaDR",
                "infinispan.client.hotrod.security.authentication.password": "fDJRITFeud"
        ])
        InfinispanClient client = ctx.getBean(InfinispanClient)
        InfinispanHealthIndicator healthIndicator = ctx.getBean(InfinispanHealthIndicator)

        when:
        HealthResult health = Flowable.fromPublisher(healthIndicator.result).blockingFirst()
        println health

        then:
        health.status == HealthStatus.UP
        health.details
    }

}
