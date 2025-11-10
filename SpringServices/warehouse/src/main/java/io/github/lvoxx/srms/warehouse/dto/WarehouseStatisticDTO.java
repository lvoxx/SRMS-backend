package io.github.lvoxx.srms.warehouse.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class WarehouseStatisticDTO {

    // ==================== QUANTITY STATISTICS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class QuantityResponse {
        private UUID warehouseId;
        private Long quantity;
        private String type;
        private OffsetDateTime fromDate;
        private OffsetDateTime toDate;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class BalanceResponse {
        private UUID warehouseId;
        private Long totalImport;
        private Long totalExport;
        private Long balance;
    }

    // ==================== ALERT RESPONSES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class AlertItem {
        private UUID id;
        private String productName;
        private Integer currentQuantity;
        private Integer minQuantity;
        private Integer deficit;
        
        @Pattern(regexp = "CRITICAL|WARNING|INFO", message = "{error.validation.alert.invalidSeverity}")
        private String severity;
        
        private String message;
        private OffsetDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class AlertListResponse {
        private List<AlertItem> items;
        private Long totalItems;
        
        @Min(value = 0, message = "{error.validation.pagination.pageNegative}")
        private Integer page;
        
        @Min(value = 1, message = "{error.validation.pagination.sizeNegative}")
        private Integer size;
        
        @Pattern(regexp = "BELOW_MINIMUM|OUT_OF_STOCK|ALL_ALERTS", message = "{error.validation.alert.invalidType}")
        private String alertType;
    }

    // ==================== DASHBOARD STATISTICS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class DashboardResponse {
        private Long totalWarehouses;
        private Long healthyWarehouses;
        private Long belowMinimum;
        private Long outOfStock;
        private Double healthPercentage;
        private Long totalTransactions;
        private Long totalImportTransactions;
        private Long totalExportTransactions;
        private OffsetDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class WarehouseDetailsResponse {
        private WarehouseDTO.Response warehouse;
        private Long totalImport;
        private Long totalExport;
        private Long balance;
        private Long transactionCount;
        private Boolean isBelowMinimum;
        private Boolean isOutOfStock;
        private OffsetDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class TimeBasedStatisticsResponse {
        private UUID warehouseId;
        private OffsetDateTime fromDate;
        private OffsetDateTime toDate;
        private Long importQuantity;
        private Long exportQuantity;
        private Long netChange;
        private OffsetDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class TimeBasedStatisticsRequest {
        @NotNull(message = "{error.validation.warehouseId.notNull}")
        private UUID warehouseId;
        
        @NotNull(message = "{error.validation.dateRange.required}")
        private OffsetDateTime fromDate;
        
        @NotNull(message = "{error.validation.dateRange.required}")
        private OffsetDateTime toDate;
    }

    // ==================== WEBSOCKET EVENT TYPES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class WebSocketEvent {
        @NotNull(message = "{error.validation.event.typeRequired}")
        @Pattern(regexp = "DASHBOARD_UPDATE|ALERT_UPDATE|WAREHOUSE_UPDATE", message = "{error.validation.event.invalidFormat}")
        private String eventType;
        
        @NotNull(message = "{error.validation.event.dataRequired}")
        private Object data;
        
        private OffsetDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class AlertNotification {
        @NotNull(message = "{error.validation.warehouseId.notNull}")
        private UUID warehouseId;
        
        @NotNull(message = "{error.validation.productName.notBlank}")
        private String productName;
        
        @NotNull(message = "{error.validation.alert.invalidType}")
        @Pattern(regexp = "OUT_OF_STOCK|BELOW_MINIMUM", message = "{error.validation.alert.invalidType}")
        private String alertType;
        
        @NotNull(message = "{error.validation.alert.invalidSeverity}")
        @Pattern(regexp = "CRITICAL|WARNING", message = "{error.validation.alert.invalidSeverity}")
        private String severity;
        
        @NotNull(message = "{error.validation.notification.messageRequired}")
        private String message;
        
        private Integer currentQuantity;
        private Integer minQuantity;
        private OffsetDateTime timestamp;
    }
}