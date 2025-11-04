package io.github.lvoxx.srms.warehouse.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
        private String severity; // CRITICAL, WARNING, INFO
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
        private Integer page;
        private Integer size;
        private String alertType; // BELOW_MINIMUM, OUT_OF_STOCK, ALL_ALERTS
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

    // ==================== WEBSOCKET EVENT TYPES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class WebSocketEvent {
        private String eventType; // DASHBOARD_UPDATE, ALERT_UPDATE, WAREHOUSE_UPDATE
        private Object data;
        private OffsetDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class AlertNotification {
        private UUID warehouseId;
        private String productName;
        private String alertType; // OUT_OF_STOCK, BELOW_MINIMUM
        private String severity; // CRITICAL, WARNING
        private String message;
        private Integer currentQuantity;
        private Integer minQuantity;
        private OffsetDateTime timestamp;
    }
}
