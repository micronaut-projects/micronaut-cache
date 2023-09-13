package io.micronaut.cache;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class NewsControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Timeout(4)
    @Test
    void fetchingOctoberHeadlinesUsesCache() {
        HttpRequest<?> request = HttpRequest.GET(UriBuilder.of("/").path(Month.OCTOBER.toString()).build());

        News news = client.toBlocking().retrieve(request, News.class);
        String expected = "Micronaut AOP: Awesome flexibility without the complexity";
        assertEquals(List.of(expected), news.getHeadlines());

        news = client.toBlocking().retrieve(request, News.class);
        assertEquals(List.of(expected), news.getHeadlines());
    }
}
