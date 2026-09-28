plugins {
    id("io.micronaut.build.internal.cache-test-suite")
    id("java-library")
    id("io.micronaut.build.internal.python")
}

// The examples of the guide, in Python, compiled by the Python compiler (micronaut-inject-python). The compiler
// takes the (jar-resolved) compile classpath as its annotation processor path, so the Micronaut processors are
// regular test dependencies rather than annotationProcessor ones. The Python tests only run with -Ppython-ci.
dependencies {
    // The Java test helper (the @ContextConfigurer starting the Hazelcast container) is processed by javac
    testAnnotationProcessor(mn.micronaut.inject.java)

    testImplementation(mn.micronaut.inject.python.test)
    testImplementation(mn.micronaut.context.python)

    testImplementation(projects.micronautCacheHazelcast)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(platform(mnTestResources.boms.testcontainers))
    testImplementation(libs.testcontainers.junit)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.suite)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("hazelcastVersion", libs.versions.managed.hazelcast.get())
    systemProperty("micronaut.python.pool.enabled", "false")
}
