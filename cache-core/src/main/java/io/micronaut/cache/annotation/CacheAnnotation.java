package io.micronaut.cache.annotation;

import io.micronaut.aop.Around;
import io.micronaut.cache.interceptor.CacheInterceptor;
import io.micronaut.context.annotation.Type;

import java.lang.annotation.*;

/**
 * Meta annotation to mark cache operations
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 2.3.0
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Around
@Type(CacheInterceptor.class)
public @interface CacheAnnotation {
}
