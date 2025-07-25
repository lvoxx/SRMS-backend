package io.github.lvoxx.srms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    KeyResolver apiKeyResolver() {
        return exchange -> {
            // Rate limit by API key if present
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                return Mono.just(apiKey);
            }

            // Otherwise rate limit by JWT subject
            return exchange.getPrincipal()
                    .cast(JwtAuthenticationToken.class)
                    .map(token -> token.getToken().getSubject())
                    .defaultIfEmpty("anonymous");
        };
    }
}