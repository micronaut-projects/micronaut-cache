package io.micronaut.cache

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Status

@Controller
class SimpleController {

    private final CounterService counterService

    SimpleController(CounterService counterService) {
        this.counterService = counterService
    }

    @Get("/inc")
    int increment() {
        return counterService.increment("test")
    }

    @Get("/get")
    int get() {
        return counterService.getValue("test")
    }

    @Get("/del")
    @Status(HttpStatus.FOUND)
    void del() {
        counterService.reset("test")
    }
}
