import io.micronaut.build.utils.DefaultVersions

plugins {
    id("groovy")
    id("io.micronaut.build.internal.cache-module")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(mn.micronaut.inject.groovy)

    implementation(projects.micronautCacheCore)
    implementation(mn.reactor)

    // The following dependencies are implementation and runtimeOnly
    // because it's a TCK, so it's not test dependencies for this module
    // but test dependencies for modules that would depend on the TCK!

    implementation("org.spockframework:spock-core") {
        version {
            require(DefaultVersions.SPOCK_VERSION)
        }
    }
    runtimeOnly("org.objenesis:objenesis") {
        version {
            require(DefaultVersions.OBJENESIS_VERSION)
        }
    }
}
