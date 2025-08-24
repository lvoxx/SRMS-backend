package io.github.lvoxx.srms.common.exception;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Builder.Default;

@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(toBuilder = true)
public class ErrorResponse {
    private final String message;
    private final String details;
    private final int status;
    @Default
    private final LocalDateTime timestamp = LocalDateTime.now();
}