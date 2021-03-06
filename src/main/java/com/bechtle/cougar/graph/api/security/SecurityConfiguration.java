package com.bechtle.cougar.graph.api.security;

import com.bechtle.cougar.graph.features.multitenancy.security.ApplicationAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@Profile("! test")
@Slf4j(topic = "cougar.graph.security")
public class SecurityConfiguration {


    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String SUBSCRIPTION_KEY_HEADER = "X-SUBSCRIPTION-KEY";

    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         List<ReactiveAuthenticationManager> authenticationManager,
                                                         ServerAuthenticationConverter authenticationConverter) {
        final ReactiveAuthenticationManager authenticationManagers = new DelegatingReactiveAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);

        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .authorizeExchange()
                    .matchers(EndpointRequest.to("info","env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(AdminAuthentication.ADMIN_AUTHORITY)
                    .matchers(EndpointRequest.to("health")).permitAll()
                    .pathMatchers("/api/admin/**").hasAuthority(AdminAuthentication.ADMIN_AUTHORITY)
                    .pathMatchers("/api/**").hasAnyAuthority(ApplicationAuthentication.USER_AUTHORITY, AdminAuthentication.ADMIN_AUTHORITY)
                    .anyExchange().permitAll()
                .and()
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .build();

    }


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    ServerAuthenticationConverter buildAuthenticationConverter() {
        return exchange -> {
            List<String> apiKeys = exchange.getRequest().getHeaders().get(API_KEY_HEADER);
            if (apiKeys == null || apiKeys.size() == 0) {
                // lets fallback to username/password
                return Mono.empty();
            }

            log.trace("Extracting API Key from request headers");
            ApiKeyToken apiKeyToken = new ApiKeyToken(apiKeys.get(0));

            exchange.getRequest().getHeaders().getValuesAsList(SUBSCRIPTION_KEY_HEADER).forEach(key -> apiKeyToken.getDetails().setSubscriptionKey(key));
            return Mono.just(apiKeyToken);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    ServerAuthenticationConverter buildTestingAuthenticationConverter() {
        return exchange -> Mono.just(new TestingAuthenticationToken("test", "test"));
    }




}
