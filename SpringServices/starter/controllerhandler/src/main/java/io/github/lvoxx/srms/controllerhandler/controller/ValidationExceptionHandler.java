package io.github.lvoxx.srms.controllerhandler.controller;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

        /**
         * Handles ConstraintViolationException thrown by method-level validation.
         * This is the traditional validation exception.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(
                        ConstraintViolationException ex,
                        ServerWebExchange exchange) {

                log.error("Constraint violation error: {}", ex.getMessage());

                Map<String, String> errors = ex.getConstraintViolations().stream()
                                .collect(Collectors.toMap(
                                                violation -> {
                                                        String path = violation.getPropertyPath().toString();
                                                        String[] parts = path.split("\\.");
                                                        return parts[parts.length - 1];
                                                },
                                                ConstraintViolation::getMessage,
                                                (existing, replacement) -> existing));

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(OffsetDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message("Validation failed for one or more parameters")
                                .path(exchange.getRequest().getPath().value())
                                .validationErrors(errors)
                                .build();

                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
        }

        /**
         * Handles HandlerMethodValidationException (Spring 6.1+).
         * This is the new exception type for method parameter validation in Spring
         * WebFlux.
         */
        @ExceptionHandler(HandlerMethodValidationException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleHandlerMethodValidationException(
                        HandlerMethodValidationException ex,
                        ServerWebExchange exchange) {

                log.error("Handler method validation error: {}", ex.getMessage());

                Map<String, String> errors = new HashMap<>();

                ex.getValueResults().forEach(parameterValidationResult -> {
                        String parameterName = parameterValidationResult.getMethodParameter().getParameterName();

                        parameterValidationResult.getResolvableErrors().forEach(error -> {
                                String errorMessage = error.getDefaultMessage();
                                if (errorMessage != null) {
                                        errors.put(parameterName, errorMessage);
                                } else {
                                        errors.put(parameterName, "Invalid value");
                                }
                        });
                });

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(OffsetDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message("Validation failed for one or more parameters")
                                .path(exchange.getRequest().getPath().value())
                                .validationErrors(errors)
                                .build();

                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException ex,
                        ServerWebExchange exchange) {

                log.error("Handler method validation error: {}", ex.getMessage());

                // Map field -> message
                Map<String, String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                fieldError -> fieldError.getField(),
                                                fieldError -> fieldError.getDefaultMessage(),
                                                (msg1, msg2) -> msg1 // nếu trùng field thì giữ lỗi đầu tiên
                                ));

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(OffsetDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message("Validation on method failed for one or more parameters")
                                .path(exchange.getRequest().getPath().value())
                                .validationErrors(errors)
                                .build();

                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        /**
         * Handles ServerWebInputException which wraps validation exceptions in WebFlux.
         * This catches validation errors that are wrapped by Spring WebFlux.
         */
        @ExceptionHandler(ServerWebInputException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(
                        ServerWebInputException ex,
                        ServerWebExchange exchange) {

                log.error("Server web input error: {}", ex.getMessage());

                // Check if the cause is a ConstraintViolationException
                Throwable cause = ex.getCause();
                if (cause instanceof ConstraintViolationException) {
                        return handleConstraintViolationException((ConstraintViolationException) cause, exchange);
                }

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(OffsetDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getReason() != null ? ex.getReason() : "Invalid input")
                                .path(exchange.getRequest().getPath().value())
                                .build();

                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
        }

        /**
         * Handles WebExchangeBindException thrown by request body validation.
         * This occurs when validation fails on DTO fields with @Valid.
         */
        @ExceptionHandler(WebExchangeBindException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(
                        WebExchangeBindException ex,
                        ServerWebExchange exchange) {

                log.error("Request body validation error: {}", ex.getMessage());

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = error instanceof FieldError
                                        ? ((FieldError) error).getField()
                                        : error.getObjectName();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(OffsetDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message("Validation failed for request body")
                                .path(exchange.getRequest().getPath().value())
                                .validationErrors(errors)
                                .build();

                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
        }

        /**
         * Error response structure for consistent API error responses.
         */
        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ErrorResponse {
                private OffsetDateTime timestamp;
                private Integer status;
                private String error;
                private String message;
                private String path;
                private Map<String, String> validationErrors;
        }
}