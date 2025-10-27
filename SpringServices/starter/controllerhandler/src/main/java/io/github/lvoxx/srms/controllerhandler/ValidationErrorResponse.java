package io.github.lvoxx.srms.controllerhandler;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class ValidationErrorResponse {
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private List<FieldValidationError> errors;

    @Data
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class FieldValidationError {
        private String field;
        private String error;
    }
}
