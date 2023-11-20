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
    testImplementation(libs.testcontainers)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("hazelcastVersion", libs.versions.managed.hazelcast.get())
}
