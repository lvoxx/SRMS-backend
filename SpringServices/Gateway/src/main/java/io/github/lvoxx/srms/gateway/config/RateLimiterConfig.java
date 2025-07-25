package io.github.lvoxx.srms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            // Check for API key first
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                return Mono.just("api-key-" + apiKey);
            }

            // Then check for JWT
            return exchange.getPrincipal()
                    .cast(JwtAuthenticationToken.class)
                    .map(token -> {
                        String clientId = token.getToken().getClaimAsString("azp");
                        String userId = token.getToken().getSubject();
                        return "jwt-" + clientId + "-" + userId;
                    })
                    .defaultIfEmpty("anonymous");
        };
    }
}