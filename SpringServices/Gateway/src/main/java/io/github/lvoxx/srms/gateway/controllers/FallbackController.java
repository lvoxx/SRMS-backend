package io.github.lvoxx.srms.gateway.controllers;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> serviceUnavailable() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", "SERVICE_UNAVAILABLE",
                        "message", "Service is currently unavailable. Please try again later.",
                        "success", false)));
    }

    @GetMapping("/timeout")
    public Mono<ResponseEntity<Map<String, Object>>> timeout() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", "GATEWAY_TIMEOUT",
                        "message", "Request timed out. Please try again later.",
                        "success", false)));
    }

    @GetMapping("/rate-limit")
    public Mono<ResponseEntity<Map<String, Object>>> rateLimitExceeded() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", "TOO_MANY_REQUESTS",
                        "message", "Rate limit exceeded. Please slow down your requests.",
                        "success", false)));
    }

    @GetMapping("/access-denied")
    public ResponseEntity<Map<String, Object>> accessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "ACCESS_DENIED",
                        "message", "You do not have permission to perform this action.",
                        "timestamp", Instant.now()));
    }

    @GetMapping("/not-found")
    public ResponseEntity<Map<String, Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", "The requested API path was not found.",
                        "timestamp", Instant.now()));
    }
}
