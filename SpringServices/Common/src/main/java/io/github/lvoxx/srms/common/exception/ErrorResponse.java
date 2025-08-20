package io.github.lvoxx.srms.common.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private final String message;
    private final String details;
    private final int status;
    private final LocalDateTime timestamp;

    public ErrorResponse(String message, String details, int status) {
        this.message = message;
        this.details = details;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public int getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}