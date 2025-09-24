package io.github.lvoxx.srms.common.exception.model;

public class DataPersistantException extends RuntimeException {

    public DataPersistantException() {
        super();
    }

    public DataPersistantException(String message) {
        super(message);
    }

    public DataPersistantException(String message, Throwable throwable) {
        super(message, throwable);
    }
}