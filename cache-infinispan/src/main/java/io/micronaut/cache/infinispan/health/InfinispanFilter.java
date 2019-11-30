package io.micronaut.cache.infinispan.health;

import io.micronaut.cache.infinispan.InfinispanHotRodClientConfiguration;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import org.infinispan.client.hotrod.configuration.AuthenticationConfiguration;
import org.reactivestreams.Publisher;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

/**
 * TODO: javadoc
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Filter(serviceId = InfinispanClient.ID, value = "/**")
public class InfinispanFilter implements HttpClientFilter {

    private InfinispanHotRodClientConfiguration clientConfiguration;

    public InfinispanFilter(InfinispanHotRodClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        AuthenticationConfiguration authenticationConfiguration = clientConfiguration.getAuthentication().create();
        if (authenticationConfiguration.enabled()) {
            NameCallback nameCallback = new NameCallback("username");
            PasswordCallback passwordCallback = new PasswordCallback("password", false);
            Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
            try {
                authenticationConfiguration.callbackHandler().handle(callbacks);
                String username = nameCallback.getName();
                String password = String.valueOf(passwordCallback.getPassword());
                return chain.proceed(request.basicAuth(username, password));
            } catch (Exception e) {
                return chain.proceed(request);
            }
        }
        return chain.proceed(request);
    }

}
