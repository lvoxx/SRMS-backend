package io.github.lvoxx.srms.gateway.exception;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

import com.nimbusds.jose.jwk.source.RateLimitReachedException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private static final String FALLBACK_URI_PREFIX = "/fallback/code/";

    // Map exception class -> fallback code
    private static final Map<Class<? extends Throwable>, String> exceptionCodeMap = Map.ofEntries(
            Map.entry(AccessDeniedException.class, "403"),
            Map.entry(AuthenticationException.class, "401"),
            Map.entry(RateLimitReachedException.class, "429"),
            Map.entry(CallNotPermittedException.class, "503"),
            Map.entry(NotFoundException.class, "404"),
            Map.entry(MethodNotAllowedException.class, "405"),
            Map.entry(UnsupportedMediaTypeException.class, "415"),
            Map.entry(ServerErrorException.class, "500"),
            Map.entry(ServiceUnavailableException.class, "503"),
            Map.entry(TimeoutException.class, "504"),
            Map.entry(org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest.class,
                    "400"),
            Map.entry(org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                    "400"),
            Map.entry(org.springframework.web.client.HttpClientErrorException.Conflict.class, "409"),
            Map.entry(org.springframework.web.reactive.function.client.WebClientResponseException.Conflict.class,
                    "409"));

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.warn("Caught an exception from [{}] for instance [{}]",
                exchange.getRequest().getPath(), ex.getClass().getSimpleName());

        String code = exceptionCodeMap.getOrDefault(ex.getClass(), "500");
        return redirect(exchange, FALLBACK_URI_PREFIX + code);
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String path) {
        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
        exchange.getResponse().getHeaders().setLocation(URI.create(path));
        return exchange.getResponse().setComplete();
    }
}
