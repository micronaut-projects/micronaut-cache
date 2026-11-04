package io.micronaut.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Builds caches that Caffeine backs with different generated cache and node classes, whose {@code FACTORY}
 * fields Caffeine reads with a {@code VarHandle}.
 */
class CaffeineGeneratedClassesTest {

    private static final Map<String, Supplier<Caffeine<Object, Object>>> BUILDERS = Map.of(
        "unbounded", Caffeine::newBuilder,
        "maximum size", () -> Caffeine.newBuilder().maximumSize(10),
        "expire after access", () -> Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(1)),
        "expire after write", () -> Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)),
        "refresh after write", () -> Caffeine.newBuilder().refreshAfterWrite(Duration.ofMinutes(1)),
        "maximum weight", () -> Caffeine.newBuilder().maximumWeight(100).weigher((k, v) -> 1),
        "weak keys and soft values", () -> Caffeine.newBuilder().weakKeys().softValues(),
        "weak values and statistics", () -> Caffeine.newBuilder().weakValues().recordStats(),
        "maximum size, expiry and listener", () -> Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(Duration.ofMinutes(1))
            .expireAfterWrite(Duration.ofMinutes(5))
            .removalListener((k, v, cause) -> { })
    );

    @Test
    void buildsCaches() {
        assertAll(BUILDERS.entrySet().stream().map(entry -> (Executable) () -> {
            Cache<String, String> cache = entry.getValue().get().build(key -> "value");
            cache.put("key", "value");
            assertEquals("value", cache.getIfPresent("key"), entry.getKey());
        }));
    }
}
