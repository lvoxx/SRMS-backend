package io.github.lvoxx.srms.common.exception.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import io.github.lvoxx.srms.common.exception.ValidationErrorResponse;
import io.github.lvoxx.srms.common.exception.ValidationErrorResponse.FieldValidationError;
import jakarta.validation.ConstraintViolationException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

        @ExceptionHandler(WebExchangeBindException.class)
        public Mono<ResponseEntity<ValidationErrorResponse>> handleWebExchangeBindException(
                        WebExchangeBindException ex) {
                List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors()
                                .stream()
                                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                                .toList();

                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .message("Validation Failure")
                                .status(HttpStatus.BAD_REQUEST.value())
                                .timestamp(LocalDateTime.now())
                                .errors(errors)
                                .build();

                return Mono.just(ResponseEntity.badRequest().body(response));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public Mono<ResponseEntity<ValidationErrorResponse>> handleConstraintViolation(
                        ConstraintViolationException ex) {
                List<FieldValidationError> errors = ex.getConstraintViolations()
                                .stream()
                                .map(cv -> new FieldValidationError(cv.getPropertyPath().toString(), cv.getMessage()))
                                .toList();

                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .message("Constraint Violation Failure")
                                .status(HttpStatus.BAD_REQUEST.value())
                                .timestamp(LocalDateTime.now())
                                .errors(errors)
                                .build();

                return Mono.just(ResponseEntity.badRequest().body(response));
        }
}
