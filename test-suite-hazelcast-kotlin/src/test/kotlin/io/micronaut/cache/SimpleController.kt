package io.micronaut.cache

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Status

@Controller
class SimpleController(private val counterService: CounterService) {

    @Get("/inc")
    fun increment() = counterService.increment("test")

    @Get("/get")
    fun get() = counterService.getValue("test")

    @Get("/del")
    @Status(HttpStatus.FOUND)
    fun del() = counterService.reset("test")
}
