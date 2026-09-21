from micronaut.http import HttpStatus
from micronaut.http.annotation import Controller, Get, Status

from .CounterService import CounterService


@Controller
class SimpleController:

    def __init__(self, counter_service: CounterService):
        self.counter_service = counter_service

    @Get("/inc")
    def increment(self) -> int:
        return self.counter_service.increment("test")

    @Get("/get")
    def get(self) -> int:
        return self.counter_service.get_value("test")

    @Get("/del")
    @Status(HttpStatus.FOUND)
    def delete(self) -> None:
        self.counter_service.reset("test")
