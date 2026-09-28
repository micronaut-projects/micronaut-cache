from jakarta.inject import Singleton
from micronaut.cache.annotation import CacheConfig, CacheInvalidate, CachePut, Cacheable


@Singleton
@CacheConfig(cacheNames=["counter"])
class CounterService:

    def __init__(self):
        self.counters: dict[str, int] = {}

    @CachePut
    def increment(self, name: str) -> int:
        value = self.counters.get(name, 0) + 1
        self.counters[name] = value
        return value

    @Cacheable
    def get_value(self, name: str) -> int:
        return self.counters.setdefault(name, 0)

    @CacheInvalidate
    def reset(self, name: str) -> None:
        self.counters.pop(name, None)

    @CacheInvalidate
    def set(self, name: str, val: int) -> None:
        self.counters[name] = val
