package io.micronaut.cache;

import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@MicronautTest(startApplication = false, transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Requires(property = "spec.name", value = "MockBeanCacheableTest")
class MockBeanCacheableTest {

    @Inject
    SomeClassWithCacheable someClass;

    @Inject
    SomeClassWithCacheable mockedSomeClass;

    @Test
    @Order(1)
    void case1() {
        reset(mockedSomeClass);
        when(mockedSomeClass.test("shared-key")).thenReturn("case-1");

        assertEquals("case-1", someClass.test("shared-key"));
    }

    @Test
    @Order(2)
    void case2() {
        reset(mockedSomeClass);
        when(mockedSomeClass.test("shared-key")).thenReturn("case-2");

        assertEquals("case-1", someClass.test("shared-key"));
        verifyNoInteractions(mockedSomeClass);
    }

    @MockBean(SomeClassWithCacheable.class)
    SomeClassWithCacheable mockSomeClassWithCacheable() {
        return mock(SomeClassWithCacheable.class);
    }
}

@Singleton
@Requires(property = "spec.name", value = "MockBeanCacheableTest")
@CacheConfig(cacheNames = {"mock-bean-cacheable"})
class SomeClassWithCacheable {

    @Cacheable(parameters = {"param"})
    String test(String param) {
        return param;
    }
}
