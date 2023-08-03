package io.micronaut.cache.serialize

import io.micronaut.core.convert.DefaultMutableConversionService
import spock.lang.Issue
import spock.lang.PendingFeature
import spock.lang.Specification

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DefaultStringKeySerializerSpec extends Specification {

    @PendingFeature(reason = "Depends on https://github.com/micronaut-projects/micronaut-core/pull/8963")
    @Issue("https://github.com/micronaut-projects/micronaut-cache/issues/404")
    void "test TemporalAccessor serialization"() {

        given:
        final serializer = new DefaultStringKeySerializer("cacheName", Charset.defaultCharset(), new DefaultMutableConversionService())

        expect:
        new String(serializer.serialize(sourceObject).get(), Charset.defaultCharset()) == "cacheName:$result"

        where:
        sourceObject                                                            | result
        LocalDate.of(2022, 8, 12)                                               | "2022-08-12"
        LocalDateTime.of(2022, 8, 12, 12, 19)                                   | "2022-08-12T12:19:00"
        OffsetTime.of(12, 19, 0, 0, ZoneOffset.ofHours(5))                      | "12:19:00+05:00"
        OffsetDateTime.of(2022, 8, 12, 12, 19, 0, 0, ZoneOffset.ofHours(5))     | "2022-08-12T12:19:00+05:00"
        ZonedDateTime.of(2022, 8, 12, 12, 19, 0, 0, ZoneOffset.ofHours(5))      | "2022-08-12T12:19:00+05:00"
        ZonedDateTime.of(2022, 8, 12, 12, 19, 0, 0, ZoneId.of("Europe/Berlin")) | "2022-08-12T12:19:00+02:00[Europe/Berlin]"
        LocalDateTime.of(2022, 8, 12, 12, 19).toInstant(ZoneOffset.UTC)         | "2022-08-12T12:19:00Z"
    }
}
