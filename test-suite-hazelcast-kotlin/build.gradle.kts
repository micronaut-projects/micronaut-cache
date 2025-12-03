plugins {
    id("io.micronaut.build.internal.cache-test-suite-kotlin")
}

repositories {
    mavenCentral()
}

dependencies {
    kaptTest(mn.micronaut.inject.java)

    testImplementation(projects.micronautCacheHazelcast)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.kotlinx.coroutines.core)
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
}
