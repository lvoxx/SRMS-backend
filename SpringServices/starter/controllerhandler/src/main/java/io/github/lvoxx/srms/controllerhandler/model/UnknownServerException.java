package io.github.lvoxx.srms.controllerhandler.model;

public class UnknownServerException extends RuntimeException {

    public UnknownServerException() {
        super();
    }

    public UnknownServerException(String message) {
        super(message);
    }

    public UnknownServerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}