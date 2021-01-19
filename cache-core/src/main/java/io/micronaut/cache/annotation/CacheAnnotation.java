package io.micronaut.cache.annotation;

import io.micronaut.aop.Around;
import io.micronaut.cache.interceptor.CacheInterceptor;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;

import java.lang.annotation.*;

/**
 * Meta annotation to mark cache operations
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 2.3.0
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Around
@Type(CacheInterceptor.class)
@Internal
public @interface CacheAnnotation {
}
