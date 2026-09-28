from typing import Annotated

from jakarta.inject import Inject
from java.lang import Integer
from micronaut.context.annotation import Property
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ConditionalService import ConditionalService, Id
from .MyRemovalHandler import MyRemovalHandler

@MicronautTest
@Property(name="micronaut.caches.counter.initial-capacity", value="10")
@Property(name="micronaut.caches.counter.test-mode", value="true")
@Property(name="micronaut.caches.counter.maximum-size", value="20")
@Property(name="micronaut.caches.counter.listen-to-removals", value="true")
class CaffeineTest:

    http_client: Annotated[HttpClient, Inject, Client("/")]
    removal_handler: Annotated[MyRemovalHandler, Inject]
    conditional_service: Annotated[ConditionalService, Inject]

    @Test
    def test_simple(self) -> None:
        client = self.http_client.toBlocking()
        bean = self.removal_handler

        inc = client.retrieve("/inc", Integer)
        assert inc == 1

        assert bean.get_removals() == []

        inc = client.retrieve("/inc", Integer)
        assert inc == 2

        assert bean.get_removals() == ["test|1|REPLACED"]

        get = client.retrieve("/get", Integer)
        assert get == 2

        client.exchange("/del")

        assert bean.get_removals() == ["test|1|REPLACED", "test|2|EXPLICIT"]

    @Test
    def test_conditional(self) -> None:
        bean = self.conditional_service

        # Get the same thing twice, ids > 5 are cached
        first = [bean.get(Id(i)) for i in range(1, 11)]
        second = [bean.get(Id(i)) for i in range(1, 11)]

        assert first[0:5] != second[0:5]
        assert first[5:10] == second[5:10]
