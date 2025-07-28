package io.github.lvoxx.srms.gateway.exception;

import java.net.URI;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.nimbusds.jose.jwk.source.RateLimitReachedException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final String fallbackUri = "/fallback/code/";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof AccessDeniedException) {
            return redirect(exchange, fallbackUri + "403");
        } else if (ex instanceof RateLimitReachedException) {
            return redirect(exchange, fallbackUri + "429");
        } else if (ex instanceof CallNotPermittedException) {
            return redirect(exchange, fallbackUri + "503");
        } else if (ex instanceof NotFoundException) {
            return redirect(exchange, fallbackUri + "404");
        }
        return redirect(exchange, fallbackUri + "500");
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String path) {
        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
        exchange.getResponse().getHeaders().setLocation(URI.create(path));
        return exchange.getResponse().setComplete();
    }
}