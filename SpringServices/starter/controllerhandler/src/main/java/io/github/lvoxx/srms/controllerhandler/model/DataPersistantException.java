package io.github.lvoxx.srms.controllerhandler.model;

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