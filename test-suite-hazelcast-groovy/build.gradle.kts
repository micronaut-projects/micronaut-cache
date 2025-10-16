plugins {
    id("groovy")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(projects.micronautCacheHazelcast)

    testImplementation(mn.micronaut.inject.groovy)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mnTestResources.testcontainers.core)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.suite)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("hazelcastVersion", libs.versions.managed.hazelcast.get())
}
