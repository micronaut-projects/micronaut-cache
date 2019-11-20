# Contributing a new cache implementation

How a cache implementation should be created depends on whether the caches can/should be declared ahead of time. For caches with predefined cache names, `SyncCache` beans should be created which will be ingested by the default cache manager. For caches that cannot or should not be defined ahead of time, a `DynamicCacheManager` bean should be registered to retrieve the cache.

Any other beans created are not used by Micronaut. The only beans referenced are `SyncCache` and `DynamicCacheManager`.

## Synchronous cache implementations

For caches that using blocking IO, the `getExecutorService()` method should be overridden for each `SyncCache` to allow the offloading of the operations to a thread pool. Unless there is a reason to do otherwise, the `TaskManagers.IO` named thread pool should be used.

## Asynchronous cache implementations

If the cache provider has an asynchronous API, the `async()` method of `SyncCache` should be overridden to supply an `AsyncCache` that uses the native asynchronous API. See the Hazelcast implementation for an example.

## Gradle build and setup

To ensure consistency across implementations, copy the build of an existing implementation and alter the dependencies as needed.

## Documentation

All cache implementations should be documented by modifying the `src/main/docs/guide/toc.yml` to include your documentation. Documentation should describe how to install and configure the implementation.



