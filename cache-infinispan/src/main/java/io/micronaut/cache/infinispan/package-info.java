/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Configuration
@Requires(property = "infinispan.enabled", notEquals = StringUtils.FALSE)
package io.micronaut.cache.infinispan;

import io.micronaut.context.annotation.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;