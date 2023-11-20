package io.micronaut.cache;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;

@Singleton
@CacheConfig(cacheNames = {"conditional"})
public class ConditionalService {

    DocRepo repository = new DocRepo();

    // tag::conditional[]
    public record Id(Integer value) {
    }

    @Cacheable(condition = "#{id.value > 5}")
    public String get(Id id) {
        return repository.get(id);
    }
    // end::conditional[]

    // here to make the docs look nice when we extract the function above
    private static class DocRepo {

        String get(Id id) {
            return "test " + id.value + " " + System.currentTimeMillis();
        }
    }
}
