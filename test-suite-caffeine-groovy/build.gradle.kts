plugins {
    id("groovy")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(projects.micronautCacheCaffeine)

    testImplementation(mn.micronaut.inject.groovy)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.spock)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
