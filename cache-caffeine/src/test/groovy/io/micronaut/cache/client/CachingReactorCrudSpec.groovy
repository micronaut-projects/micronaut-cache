/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.client;

import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class CachingReactorCrudSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(
            'micronaut.caches.books.maximum-size': 20,
            'micronaut.caches.book-list.maximum-size': 20
    )

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    void "test it is possible to implement CRUD operations with Reactor"() {
        given:
        BookClient client = context.getBean(BookClient)
        BookController bookController = context.getBean(BookController)

        when:
        Book book = client.get(99).block()
        List<Book> books = client.list().block()

        then:
        book == null
        books.size() == 0

        when:
        book = client.save("The Stand").block()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1

        when:
        book = client.get(book.id).block()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1
        bookController.getInvocationCount.get() == 2


        when:
        book = client.get(book.id).block()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1
        bookController.getInvocationCount.get() == 2

        when:'the full response is resolved'
        HttpResponse<Book> bookAndResponse = client.getResponse(book.id).block()

        then:"The response is valid"
        bookAndResponse.status() == HttpStatus.OK
        bookAndResponse.body().title == "The Stand"

        when:
        book = client.update(book.id, "The Shining").block()

        then:
        book != null
        book.title == "The Shining"
        book.id == 1

        when:
        book = client.get(book.id).block()

        then:
        book != null
        book.title == "The Shining"
        book.id == 1
        bookController.getInvocationCount.get() == 3

        when:
        book = client.delete(book.id).block()

        then:
        book != null

        when:
        book = client.get(book.id).block()

        then:
        book == null
    }


    @Client('/reactor/caching/books')
    static interface BookClient extends BookApi {
    }

    @Controller("/reactor/caching/books")
    static class BookController implements BookApi {

        Map<Long, Book> books = new LinkedHashMap<>()
        AtomicLong currentId = new AtomicLong(0)
        AtomicInteger getInvocationCount = new AtomicInteger()

        @Override
        Mono<Book> get(Long id) {
            Mono.create(new Consumer<MonoSink<Book>>() {
                @Override
                void accept(MonoSink<Book> monoSink) {
                    getInvocationCount.incrementAndGet()
                    Book book = books.get(id)
                    if(book) {
                        monoSink.success(book)
                    } else {
                        monoSink.success()
                    }
                }
            })
        }

        @Override
        Mono<HttpResponse<Book>> getResponse(Long id) {
            Book book = books.get(id)
            if(book) {
                return Mono.just(HttpResponse.ok(book))
            }
            return Mono.just(HttpResponse.notFound())
        }

        @Override
        Mono<List<Book>> list() {
            return Mono.just(books.values().toList())
        }

        @Override
        Mono<Book> delete(Long id) {
            Book book = books.remove(id)
            if(book) {
                return Mono.just(book)
            }
            return Mono.empty()
        }

        @Override
        Mono<Book> save(String title) {
            Book book = new Book(title: title, id:currentId.incrementAndGet())
            books[book.id] = book
            return Mono.just(book)
        }

        @Override
        Mono<Book> update(Long id, String title) {
            Book book = books[id]
            if(book != null) {
                book.title = title
                return Mono.just(book)
            }
            else {
                return Mono.empty()
            }
        }
    }

    static interface BookApi {

        @Get("/{id}")
        @Cacheable("books")
        Mono<Book> get(Long id)

        @Get("/res/{id}")
        @Cacheable("books")
        Mono<HttpResponse<Book>> getResponse(Long id)

        @Get
        @Cacheable("book-list")
        Mono<List<Book>> list()

        @Delete("/{id}")
        @CacheInvalidate("books")
        Mono<Book> delete(Long id)

        @Post
        @CachePut("books")
        Mono<Book> save(String title)

        @Patch("/{id}")
        @CacheInvalidate("books")
        Mono<Book> update(Long id, String title)
    }


    static class Book {
        Long id
        String title
    }
}

