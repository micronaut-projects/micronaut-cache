package io.micronaut.cache;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Singleton
@CacheConfig(cacheNames = {"conditional"})
@Requires(property = "spec.name", value = "ConditionalCacheSpec")
public class ConditionalService {

    DocRepo repsoitory = new DocRepo();

    // tag::conditional[]
    @Cacheable(condition = "#{id > 5}")
    public String get(Integer id) {
        return repsoitory.get(id);
    }
    // end::conditional[]

    // here to make the docs look nice when we extract the function above
    private static class DocRepo {

        String get(Integer id) {
            return "test " + id + " " + System.currentTimeMillis();
        }
    }
}
