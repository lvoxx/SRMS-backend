package io.github.lvoxx.srms.common.exception.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.lvoxx.srms.common.exception.ErrorResponse;
import io.github.lvoxx.srms.common.exception.model.BadRequestException;
import io.github.lvoxx.srms.common.exception.model.ConflictException;
import io.github.lvoxx.srms.common.exception.model.NotFoundException;
import io.github.lvoxx.srms.common.exception.model.ValidationException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý NotFoundException
    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFoundException(NotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "Resource Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    // Xử lý BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadRequestException(BadRequestException ex) {
        ErrorResponse error = new ErrorResponse(
                "Bad Request",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    // Xử lý ConflictException
    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflictException(ConflictException ex) {
        ErrorResponse error = new ErrorResponse(
                "Conflict",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    // Xử lý ValidationException
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
                "Validation Failed",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    // Xử lý lỗi chung (Internal Server Error)
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    // Xử lý TimeoutException trong reactive
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeoutException(java.util.concurrent.TimeoutException ex) {
        ErrorResponse error = new ErrorResponse(
                "Request Timeout",
                "The operation timed out",
                HttpStatus.REQUEST_TIMEOUT.value());
        return Mono.just(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(error));
    }
}