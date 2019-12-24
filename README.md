# Micronaut Cache

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.cache/micronaut-cache-ehcache.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.cache%22%20AND%20a:%22micronaut-cache-ehcache%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-cache/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-cache/actions)

This project includes caching support for [Micronaut](http://micronaut.io).

## Documentation

See the [stable](https://micronaut-projects.github.io/micronaut-cache/latest/guide) or [snapshot](https://micronaut-projects.github.io/micronaut-cache/snapshot/guide) documentation for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-cache/snapshot/guide) for the 
current development docs.

## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://oss.jfrog.org/artifactory/oss-snapshot-local/) using [Github Actions](https://github.com/micronaut-projects/micronaut-openapi/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-cache/actions).

A release is performed with the following steps:

* [Edit the version](https://github.com/micronaut-projects/micronaut-cache/edit/master/gradle.properties) specified by `projectVersion` in `gradle.properties` to a semantic, unreleased version. Example `1.0.0`
* [Create a new release](https://github.com/micronaut-projects/micronaut-cache/releases/new). The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-cache/actions?query=workflow%3ARelease) to check it passed successfully.
* Celebrate!
