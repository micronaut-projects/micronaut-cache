package io.micronaut.cache.infinispan.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import org.infinispan.client.hotrod.RemoteCacheManager;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Requires(beans = HealthEndpoint.class)
@Singleton
public class InfinispanHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {

    public static final String NAME = "infinispan";

    private final InfinispanClient infinispanClient;

    public InfinispanHealthIndicator(InfinispanClient infinispanClient) {
        this.infinispanClient = infinispanClient;
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        Map<String, Object> health = new HashMap<>();
        try {
            for (String s : infinispanClient.cacheManagers()) {
                health.put(s, infinispanClient.health(s));
            }
            healthStatus = HealthStatus.UP;
        } catch (Exception e) {
            healthStatus = HealthStatus.DOWN;
        }
        return health;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
