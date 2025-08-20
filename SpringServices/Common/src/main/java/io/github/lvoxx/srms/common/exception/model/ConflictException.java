package io.github.lvoxx.srms.common.exception.model;

public class ConflictException extends RuntimeException {

    public ConflictException() {
        super();
    }

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable throwable) {
        super(message, throwable);
    }
}