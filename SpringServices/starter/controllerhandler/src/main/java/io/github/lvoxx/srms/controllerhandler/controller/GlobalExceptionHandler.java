package io.github.lvoxx.srms.controllerhandler.controller;

import java.net.UnknownServiceException;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import io.github.lvoxx.srms.controllerhandler.ErrorResponse;
import io.github.lvoxx.srms.controllerhandler.message.SystemErrorMessages;
import io.github.lvoxx.srms.controllerhandler.model.BadRequestException;
import io.github.lvoxx.srms.controllerhandler.model.ConflictException;
import io.github.lvoxx.srms.controllerhandler.model.DataPersistantException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.controllerhandler.model.ValidationException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý NotFoundException
    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFoundException(RuntimeException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.NOT_FOUND.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.NOT_FOUND.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadRequestException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.BAD_REQUEST.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.BAD_REQUEST.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý ConflictException
    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflictException(RuntimeException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.CONFLICT.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.CONFLICT.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý ValidationException
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.BAD_REQUEST.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.BAD_REQUEST.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý cụ thể đã biết từ server (Internal Server Error)
    @ExceptionHandler({ DataPersistantException.class, NullPointerException.class, DataAccessException.class })
    public Mono<ResponseEntity<ErrorResponse>> handleServerException(RuntimeException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.INTERNAL_SERVER_ERROR.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.INTERNAL_SERVER_ERROR.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý lỗi 404 (Route Not Found)
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getReason() != null ? ex.getReason() : "Route Not Found")
                .details(ex.getMessage())
                .status(ex.getStatusCode().value())
                .build();

        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(error));
    }

    // Xử lý lỗi chung đã biết vị trí  (Internal Server Error)
    @ExceptionHandler(UnknownServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnknownException(UnknownServiceException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.INTERNAL_SERVER_ERROR.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.INTERNAL_SERVER_ERROR.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }
    
    // Xử lý lỗi chung (Internal Server Error)
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.INTERNAL_SERVER_ERROR.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.INTERNAL_SERVER_ERROR.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // Xử lý TimeoutException trong reactive
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeoutException(java.util.concurrent.TimeoutException ex) {
        HttpStatus status = HttpStatus.REQUEST_TIMEOUT;
        ErrorResponse error = ErrorResponse.builder()
                .message(SystemErrorMessages.REQUEST_TIMEOUT.getTitle())
                .details(Optional.ofNullable(ex.getMessage())
                        .orElseGet(() -> SystemErrorMessages.REQUEST_TIMEOUT.getDefaultDetails()))
                .status(status.value())
                .build();
        return Mono.just(ResponseEntity.status(status).body(error));
    }
}