package io.github.lvoxx.srms.customer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

public abstract class CustomerDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Request {
        @NotBlank(message = "{error.validation.firstName.notBlank}")
        @Size(max = 50, message = "{error.validation.firstName.size}")
        private String firstName;

        @NotBlank(message = "{error.validation.lastName.notBlank}")
        @Size(max = 50, message = "{error.validation.lastName.size}")
        private String lastName;

        @NotBlank(message = "{error.validation.phoneNumber.notBlank}")
        @Size(max = 20, message = "{error.validation.phoneNumber.size}")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{error.validation.phoneNumber.pattern}")
        private String phoneNumber;

        @Size(max = 100, message = "{error.validation.email.size}")
        @Pattern(regexp = "^(?:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|)$", message = "{error.validation.email.pattern}")
        private String email;

        private String[] dietaryRestrictions;
        private String[] allergies;

        @Default
        private boolean isRegular = false;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Response {
        private UUID id;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private String[] dietaryRestrictions;
        private String[] allergies;
        private boolean isRegular;
        private String notes;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime deletedAt;
    }
}
