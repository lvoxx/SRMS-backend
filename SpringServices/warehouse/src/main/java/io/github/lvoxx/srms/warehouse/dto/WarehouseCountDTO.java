package io.github.lvoxx.srms.warehouse.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class WarehouseCountDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class CountResponse {
        private Long count;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class StatisticsResponse {
        private Long totalWarehouses;
        private Long inStock;
        private Long outOfStock;
        private Long belowMinimum;
        private Long totalHistoryEntries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class HealthMetricsResponse {
        private Long totalWarehouses;
        private Long belowMinimum;
        private Long outOfStock;
        private Double belowMinimumPercentage;
        private Double outOfStockPercentage;
        private Double healthyPercentage;
    }
}