plugins {
    id("io.micronaut.build.internal.cache-test-suite")
    id("java-library")
    id("io.micronaut.build.internal.python")
}

// The examples of the guide, in Python, compiled by the Python compiler (micronaut-inject-python). The compiler
// takes the (jar-resolved) compile classpath as its annotation processor path, so the Micronaut processors are
// regular test dependencies rather than annotationProcessor ones. The Python tests only run with -Ppython-ci.
dependencies {
    testImplementation(mn.micronaut.inject.python.test)
    testImplementation(mn.micronaut.context.python)

    testImplementation(projects.micronautCacheCaffeine)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.junit5)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.suite)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("micronaut.python.pool.enabled", "false")
}
