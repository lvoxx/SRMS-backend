package io.github.lvoxx.srms.customer.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class CustomerDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "First name cannot be empty")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @NotBlank(message = "Last name cannot be empty")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @NotBlank(message = "Phone number cannot be empty")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
        private String phoneNumber;

        @Size(max = 100, message = "Email must not exceed 100 characters")
        @Pattern(regexp = "^(?:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|)$", message = "Invalid email format")
        private String email;

        private List<String> dietaryRestrictions;
        private List<String> allergies;

        private boolean isRegular;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String id;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private List<String> dietaryRestrictions;
        private List<String> allergies;
        private boolean isRegular;
        private String notes;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime deletedAt;
    }
}
