plugins {
    id("io.micronaut.build.internal.cache-test-suite")
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
//TODO remove once Micronaut Test ships Spock version compatible with Groovy 5
configurations.all {
    resolutionStrategy {
        force("org.spockframework:spock-core:2.4-M7-groovy-5.0")
    }
}
