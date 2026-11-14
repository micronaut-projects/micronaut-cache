/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.dev;

import io.micronaut.cache.CacheManager;
import io.micronaut.context.ApplicationContext;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an application with a cached service through the development runtime and edits the service: the
 * next generation caches what the edited code computes, and nothing of caching keeps a retired generation
 * reachable.
 */
class CacheReloadTest {

    private static final String GREETER = """
        package example;

        @jakarta.inject.Singleton
        @io.micronaut.cache.annotation.CacheConfig("greetings")
        public class Greeter {
            private int calls;

            @io.micronaut.cache.annotation.Cacheable
            public String greet(String name) {
                calls++;
                return "%s " + name;
            }

            public int calls() {
                return calls;
            }
        }
        """;

    @TempDir
    Path project;

    @Test
    void cachedValuesFollowAReloadAndLeaveNoRetiredGenerationReachable() {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            harness.property("micronaut.caches.greetings.maximum-size", "10");
            harness.source("example.Greeter", GREETER.formatted("Hello"));
            harness.start();
            assertReloaderPresent(harness.context());

            assertEquals("Hello Fred", greet(harness.context(), "Fred"));
            assertEquals("Hello Fred", greet(harness.context(), "Fred"));
            assertEquals(1, calls(harness.context()));
            ReloadTck.assertFollowsReload(harness, CacheReloadTest::greeter);

            harness.source("example.Greeter", GREETER.formatted("Goodbye"));
            harness.reload();
            assertReloaderPresent(harness.context());

            // the value cached by the retired generation is not served
            assertEquals("Goodbye Fred", greet(harness.context(), "Fred"));
            assertEquals("Goodbye Fred", greet(harness.context(), "Fred"));
            assertEquals(1, calls(harness.context()));
            assertTrue(harness.context().getBean(CacheManager.class).getCacheNames().contains("greetings"));
            ReloadTck.assertFollowsReload(harness, CacheReloadTest::greeter);

            // neither the caches, the interceptor nor the development-only reloader keep the first generation reachable
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    private static void assertReloaderPresent(ApplicationContext context) {
        // the bean that invalidates the caches exists in development mode only
        assertTrue(context.containsBean(type(context, "io.micronaut.cache.DevelopmentCacheReloader")));
    }

    private static Object greeter(ApplicationContext context) {
        return context.getBean(type(context, "example.Greeter"));
    }

    private static String greet(ApplicationContext context, String name) {
        Object greeter = greeter(context);
        try {
            return (String) type(context, "example.Greeter").getMethod("greet", String.class).invoke(greeter, name);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot greet", e);
        }
    }

    private static int calls(ApplicationContext context) {
        Object greeter = greeter(context);
        try {
            return (Integer) type(context, "example.Greeter").getMethod("calls").invoke(greeter);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot count the calls", e);
        }
    }

    private static Class<?> type(ApplicationContext context, String className) {
        try {
            return Class.forName(className, true, context.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(className + " is not in the application", e);
        }
    }
}
