plugins {
    id("io.micronaut.build.internal.cache-test-suite-kotlin")
}

dependencies {
    kaptTest(mn.micronaut.inject.java)

    testImplementation(projects.micronautCacheCaffeine)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.kotlinx.coroutines.core)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.junit5)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.suite)
}
