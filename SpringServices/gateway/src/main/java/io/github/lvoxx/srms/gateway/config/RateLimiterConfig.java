package io.github.lvoxx.srms.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    KeyResolver apiKeyResolver() {
        return exchange -> {
            // 1. Check X-API-KEY
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                log.info("Using API key for rate limiting: {}", apiKey);
                return Mono.just("api-key-" + apiKey);
            }

            // 2. Try JWT
            return ReactiveSecurityContextHolder.getContext()
                    .map(context -> {
                        Authentication auth = context.getAuthentication();
                        if (auth instanceof JwtAuthenticationToken jwtAuth) {
                            String clientId = jwtAuth.getToken().getClaimAsString("azp");
                            String userId = jwtAuth.getToken().getSubject();
                            if (clientId == null || userId == null) {
                                log.warn("Missing JWT claims (clientId: {}, userId: {}), falling back to IP", clientId,
                                        userId);
                                return "ip-" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                            }
                            String key = "jwt-" + clientId + "-" + userId;
                            log.info("Using JWT key for rate limiting: {}", key);
                            return key;
                        } else {
                            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                            log.warn("No JWT authentication found, using IP: {}", ip);
                            return "ip-" + ip;
                        }
                    })
                    .switchIfEmpty(Mono.fromCallable(() -> {
                        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                        log.warn("No security context, using IP: {}", ip);
                        return "ip-" + ip;
                    }));
        };
    }
}