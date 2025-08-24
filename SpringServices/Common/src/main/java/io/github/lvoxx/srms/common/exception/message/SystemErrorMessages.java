package io.github.lvoxx.srms.common.exception.message;

public enum SystemErrorMessages {
    RESOURCE_NOT_FOUND("Resource Not Found", "The requested resource was not found on the server"),
    BAD_REQUEST("Bad Request", "The request could not be understood or was missing required parameters"),
    CONFLICT("Conflict", "A conflict occurred, such as a duplicate resource"),
    VALIDATION_FAILED("Validation Failed", "The provided data did not pass validation"),
    INTERNAL_SERVER_ERROR("Internal Server Error", "An unexpected error occurred on the server"),
    REQUEST_TIMEOUT("Request Timeout", "The operation timed out");

    private final String title;
    private final String defaultDetails;

    SystemErrorMessages(String title, String defaultDetails) {
        this.title = title;
        this.defaultDetails = defaultDetails;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultDetails() {
        return defaultDetails;
    }
}