package io.github.lvoxx.srms.warehouse.dto;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.github.lvoxx.srms.controllerhandler.model.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WarehouseSearchDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Request {
        @NotBlank(message = "{error.validation.productName.notBlank}")
        @Size(max = 255, message = "{error.validation.productName.size}")

        private String productName;
        @Min(value = 0, message = "minQuantity must be greater than or equal to 0")
        private Integer minQuantity;

        @Min(value = 0, message = "maxQuantity must be greater than or equal to 0")
        private Integer maxQuantity;

        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "createdFrom must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private String createdFrom;

        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "createdTo must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private String createdTo;

        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "updatedFrom must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private String updatedFrom;

        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?([+-]\\d{2}:\\d{2}|Z)$", message = "updatedTo must be in ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private String updatedTo;

        @Builder.Default
        @Min(value = 0, message = "page must be greater than or equal to 0")
        private Integer page = 0;

        @Builder.Default
        @Min(value = 1, message = "size must be greater than or equal to 1")
        private Integer size = 20;

        // Helper methods để parse dates
        public OffsetDateTime getParsedCreatedFrom() {
            return parseOffsetDateTimeWithDefault(createdFrom, OffsetDateTime.now().minusDays(7));
        }

        public OffsetDateTime getParsedCreatedTo() {
            return parseOffsetDateTimeWithDefault(createdTo, OffsetDateTime.now().plusDays(7));
        }

        public OffsetDateTime getParsedUpdatedFrom() {
            return parseOffsetDateTime(updatedFrom);
        }

        public OffsetDateTime getParsedUpdatedTo() {
            return parseOffsetDateTime(updatedTo);
        }

        private OffsetDateTime parseOffsetDateTime(String dateTimeStr) {
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return null;
            }
            try {
                // Xử lý URL decoding
                if (dateTimeStr.contains(" ")) {
                    String normalizedStr = dateTimeStr.replaceFirst(" ", "T").replaceFirst(" ", "+");
                    return OffsetDateTime.parse(normalizedStr);
                }
                return OffsetDateTime.parse(dateTimeStr);
            } catch (DateTimeParseException e) {
                // Nếu có lỗi parse, validation pattern sẽ bắt trước đó
                return null;
            }
        }

        private OffsetDateTime parseOffsetDateTimeWithDefault(String dateTimeStr, OffsetDateTime defaultValue) {
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return defaultValue;
            }
            return parseOffsetDateTime(dateTimeStr);
        }

        // Comprehensive validation method
        public void validate() {
            List<String> errors = new ArrayList<>();

            // Quantity validation
            if (minQuantity != null && maxQuantity != null && maxQuantity < minQuantity) {
                errors.add("maxQuantity must be greater than or equal to minQuantity");
            }

            // Date range validation
            OffsetDateTime createdFromParsed = getParsedCreatedFrom();
            OffsetDateTime createdToParsed = getParsedCreatedTo();
            if (createdFromParsed != null && createdToParsed != null && createdToParsed.isBefore(createdFromParsed)) {
                errors.add("createdTo must be after or equal to createdFrom");
            }

            OffsetDateTime updatedFromParsed = getParsedUpdatedFrom();
            OffsetDateTime updatedToParsed = getParsedUpdatedTo();
            if (updatedFromParsed != null && updatedToParsed != null && updatedToParsed.isBefore(updatedFromParsed)) {
                errors.add("updatedTo must be after or equal to updatedFrom");
            }

            // Pagination validation
            if (page != null && page < 0) {
                errors.add("page must be greater than or equal to 0");
            }

            if (size != null && (size < 1 || size > 100)) {
                errors.add("size must be between 1 and 100");
            }

            // Product name length validation
            if (productName != null && productName.length() > 100) {
                errors.add("productName must be less than 100 characters");
            }

            if (!errors.isEmpty()) {
                throw new ValidationException(String.join("; ", errors));
            }
        }

        // Utility method để log request
        public String toLogString() {
            return String.format(
                    "WarehouseSearchRequest{productName='%s', minQuantity=%s, maxQuantity=%s, " +
                            "createdFrom=%s, createdTo=%s, updatedFrom=%s, updatedTo=%s, page=%d, size=%d}",
                    productName, minQuantity, maxQuantity,
                    createdFrom, createdTo, updatedFrom, updatedTo, page, size);
        }
    }
}
