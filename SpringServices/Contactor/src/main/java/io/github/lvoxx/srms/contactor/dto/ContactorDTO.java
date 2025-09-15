package io.github.lvoxx.srms.contactor.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.github.lvoxx.srms.contactor.models.ContactorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class ContactorDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Request {

        @NotNull(message = "{contactor.contactType.notnull}")
        private ContactorType type;

        @Size(max = 100, message = "{contactor.organizationName.size}")
        private String organizationName;

        @Size(max = 50, message = "{contactor.fullname.size}")
        private String fullname;

        @NotBlank(message = "{contactor.phoneNumber.notblank}")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{contactor.phoneNumber.pattern}")
        private String phoneNumber;

        @Email(message = "{contactor.email.email}")
        @Size(max = 100, message = "{contactor.email.size}")
        private String email;

        @Size(max = 200, message = "{contactor.address.size}")
        private String address;
        @NotNull(message = "{contactor.rating.notnull}")
        private Rating rating;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Response {

        private UUID id;

        private ContactorType type;

        private String organizationName;

        private String fullName;

        private String phoneNumber;

        private String email;

        private String address;

        private Rating rating;

        private String notes;

        private OffsetDateTime deletedAt;

        private OffsetDateTime createdAt;

        private OffsetDateTime updatedAt;
    }
}