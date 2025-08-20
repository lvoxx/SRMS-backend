package io.github.lvoxx.srms.common.exception.model;

public class ValidationException extends RuntimeException {

    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}