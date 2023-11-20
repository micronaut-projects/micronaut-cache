package io.micronaut.cache;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Status;

@Controller
public class SimpleController {

    private final CounterService counterService;

    public SimpleController(CounterService counterService) {
        this.counterService = counterService;
    }

    @Get("/inc")
    public int increment() {
        return counterService.increment("test");
    }

    @Get("/get")
    public int get() {
        return counterService.getValue("test");
    }

    @Get("/del")
    @Status(HttpStatus.FOUND)
    public void del() {
        counterService.reset("test");
    }
}
