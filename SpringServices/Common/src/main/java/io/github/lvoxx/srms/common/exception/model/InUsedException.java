package io.github.lvoxx.srms.common.exception.model;

public class InUsedException extends RuntimeException {

    public InUsedException() {
        super();
    }

    public InUsedException(String message) {
        super(message);
    }

    public InUsedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}