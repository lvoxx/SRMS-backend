package io.github.lvoxx.srms.warehouse.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.github.lvoxx.srms.warehouse.models.WarehouseHistory.HistoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class WarehouseDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Request {
        @NotBlank(message = "{error.validation.productName.notBlank}")
        @Size(max = 255, message = "{error.validation.productName.size}")
        private String productName;

        @NotNull(message = "{error.validation.quantity.notNull}")
        @Min(value = 0, message = "{error.validation.quantity.min}")
        private Integer quantity;

        @NotNull(message = "{error.validation.minQuantity.notNull}")
        @Min(value = 0, message = "{error.validation.minQuantity.min}")
        private Integer minQuantity;

        private UUID contactorId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class UpdateRequest {
        @Size(max = 255, message = "{error.validation.productName.size}")
        private String productName;

        @Min(value = 0, message = "{error.validation.minQuantity.min}")
        private Integer minQuantity;

        private UUID contactorId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Response {
        private UUID id;
        private String productName;
        private Integer quantity;
        private Integer minQuantity;
        private UUID contactorId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private String lastUpdatedBy;
        private Boolean isDeleted;
        private Long version;
        private Boolean isBelowMinimum;
        private Boolean isInStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class InventoryTransactionRequest {
        @NotNull(message = "{error.validation.warehouseId.notNull}")
        private UUID warehouseId;

        @NotNull(message = "{error.validation.quantity.notNull}")
        @Min(value = 1, message = "{error.validation.quantity.min}")
        private Integer quantity;

        @NotNull(message = "{error.validation.type.notNull}")
        private HistoryType type;

        private String updatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class HistoryResponse {
        private UUID id;
        private UUID warehouseId;
        private String productName;
        private Integer quantity;
        private HistoryType type;
        private String updatedBy;
        private OffsetDateTime createdAt;
    }
}