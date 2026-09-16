from typing import Annotated

import java
from jakarta.inject import Inject
from micronaut.cache.hazelcast import HazelcastCacheManager
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

Integer = java.type("java.lang.Integer")


# The Hazelcast container address is supplied by io.micronaut.cache.support.HazelcastTestConfigurer for the
# "hazelcast" environment
@MicronautTest(environments=["hazelcast"])
class HazelcastTest:

    http_client: Annotated[HttpClient, Inject, Client("/")]
    cache_manager: Annotated[HazelcastCacheManager, Inject]

    @Test
    def test_simple(self) -> None:
        client = self.http_client.toBlocking()

        inc = client.retrieve("/inc", Integer)
        assert inc == 1

        inc = client.retrieve("/inc", Integer)
        assert inc == 2

        get = client.retrieve("/get", Integer)
        assert get == 2

        assert self.cache_manager.getCache("counter").get("test", Integer).get() == 2

        client.exchange("/del")

        assert self.cache_manager.getCache("counter").get("test", Integer).isEmpty()
