# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `test-suite-caffeine-python` and
`test-suite-hazelcast-python` that are present but disabled, or that deviate from the Java example because the
direct port does not compile or does not behave like the Java example yet. It is the bug-fixing task list for
the Python compiler (`micronaut-inject-python` / `micronaut-context-python`); every row references a
`TODO(python)` comment in the sources or a workaround described below.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow). The Hazelcast tests start a
`hazelcast/hazelcast` Testcontainers container like the Java, Kotlin and Groovy test suites.

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" test-suite-caffeine-python/src test-suite-hazelcast-python/src`.
- Last full-suite command: `./gradlew :test-suite-caffeine-python:test :test-suite-hazelcast-python:test -Ppython-ci`.
- Last full-suite result: build successful, 3 tests executed, 0 skipped, 0 failures.

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut annotations are imported from their Java package (`micronaut.cache.annotation`,
  `micronaut.context.event`, `jakarta.inject`, ...).
- The snippet classes live in the `io.micronaut.cache.docs` package in all four languages (Python sources in
  `micronaut/cache/docs/`): a Python source package cannot be the imported Java package `micronaut.cache` itself
  (the generated shim package and the source package would both write `micronaut/cache/__init__.py`).
- The cache annotations (`@Cacheable`, `@CachePut`, `@CacheInvalidate`) are method decorators of the Python beans;
  the Python compiler proxies a class with interceptor bindings on its methods, no class-level around annotation is needed.
- `ConditionalService.Id` of the Java example is a top-level `@Introspected @dataclass` `Id` in Python (nested
  classes are not bean types); the `#{id.value > 5}` condition expression reads the dataclass attribute.
- `RemovalListenerImpl` implements the generic Caffeine interface `RemovalListener[str, int]` and
  `HazelcastAdditionalSettings` implements `BeanCreatedEventListener[HazelcastClientConfiguration]`; the Java
  interface method names are kept (`onRemoval`, `onCreated`).
- The Hazelcast container address of the Java test's `TestPropertyProvider` is supplied by the Java
  `@ContextConfigurer` `io.micronaut.cache.support.HazelcastTestConfigurer` (`configure(ApplicationContext)`, gated
  on the `hazelcast` environment of `@MicronautTest(environments=["hazelcast"])`) because Micronaut Test calls
  `TestPropertyProvider` before the GraalPy runtime exists.
- The tests inject the beans as class attributes (`Annotated[HttpClient, Inject, Client("/")]`) and call the
  Python beans directly (`self.conditional_service.get(Id(i))`, intercepted by the cache advice).

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

None.

## Intentionally Unsupported Snippet Targets

None.

## java.type usages

| File | Alias | Reason |
| --- | --- | --- |
| `CaffeineTest.py`, `HazelcastTest.py` | `Integer = java.type("java.lang.Integer")` | Runtime type argument of `BlockingHttpClient.retrieve(uri, Integer)` and `SyncCache.get(key, Integer)`; imported shim classes only work as type hints. |
