import time
from dataclasses import dataclass

from jakarta.inject import Singleton
from micronaut.cache.annotation import CacheConfig, Cacheable
from micronaut.core.annotation import Introspected


# tag::conditional[]
@Introspected
@dataclass
class Id:
    value: int
# end::conditional[]


# here to make the docs look nice when we extract the function below
class DocRepo:

    def get(self, id: Id) -> str:
        return f"test {id.value} {time.time_ns() // 1_000_000}"


@Singleton
@CacheConfig(cacheNames=["conditional"])
class ConditionalService:

    def __init__(self):
        self.repository = DocRepo()
    # tag::conditional[]

    @Cacheable(condition="#{id.value > 5}")
    def get(self, id: Id) -> str:
        return self.repository.get(id)
    # end::conditional[]
