plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    kaptTest(mn.micronaut.inject.java)

    testImplementation(projects.micronautCacheHazelcast)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(libs.testcontainers.junit)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("hazelcastVersion", libs.versions.managed.hazelcast.get())
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
