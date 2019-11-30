package io.micronaut.cache.infinispan.health;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

import java.util.Map;
import java.util.Set;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Client(InfinispanClient.ID)
public interface InfinispanClient {
    String ID = "infinispan";

    @Get("/rest/v2/cache-managers/{cacheManagerName}/health")
    Map<String, Object> health(String cacheManagerName);

    @Get("/rest/v2/server/cache-managers")
    Set<String> cacheManagers();
}
