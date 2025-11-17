package io.github.lvoxx.srms.common.dto;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class SimpleRangeDateTimeDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Request {
        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "\'From Range\' must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @Builder.Default
        private String from = OffsetDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toString();

        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "\'To Range\' must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @Builder.Default
        private String to = OffsetDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toString();

        public OffsetDateTime getParsedFrom() {
            return parseOffsetDateTime(from);
        }

        public OffsetDateTime getParsedTo() {
            return parseOffsetDateTime(to);
        }

    }

    public static OffsetDateTime parseOffsetDateTime(String value) {
        // Null hoặc empty → return null
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date-time format. Expected ISO-8601: yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX",
                    ex);
        }
    }

}
