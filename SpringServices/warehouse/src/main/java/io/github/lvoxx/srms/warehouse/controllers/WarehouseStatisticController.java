package io.github.lvoxx.srms.warehouse.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import io.github.lvoxx.srms.warehouse.services.WarehouseStatisticService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST Controller for warehouse statistics and analytics.
 * <p>
 * Provides endpoints for import/export statistics, alerts, dashboard metrics,
 * and time-based analysis. All GET endpoints return HATEOAS-compliant responses.
 * Real-time data streaming is available via WebSocket endpoints (see WebSocketConfig).
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/statistic")
@RequiredArgsConstructor
public class WarehouseStatisticController {

    private final WarehouseStatisticService statisticService;

    // ==================== IMPORT/EXPORT STATISTICS ====================

    /**
     * Gets total import quantity for a warehouse.
     * 
     * @param warehouseId warehouse unique identifier
     * @return Mono emitting ResponseEntity with quantity response and HATEOAS links
     */
    @GetMapping("/import/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.QuantityResponse>>> getTotalImport(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/statistic/import/{}", warehouseId);

        return statisticService.getTotalImportQuantity(warehouseId)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getTotalImport(warehouseId)).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getTotalExport(warehouseId)).withRel("export"));
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getBalance(warehouseId)).withRel("balance"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Gets total export quantity for a warehouse.
     * 
     * @param warehouseId warehouse unique identifier
     * @return Mono emitting ResponseEntity with quantity response and HATEOAS links
     */
    @GetMapping("/export/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.QuantityResponse>>> getTotalExport(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/statistic/export/{}", warehouseId);

        return statisticService.getTotalExportQuantity(warehouseId)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getTotalExport(warehouseId)).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getTotalImport(warehouseId)).withRel("import"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Gets quantity by type and date range.
     * <p>
     * Useful for analyzing import/export patterns over specific time periods.
     * 
     * @param warehouseId warehouse unique identifier
     * @param type        transaction type (import or export)
     * @param from        start date of range (ISO 8601 format)
     * @param to          end date of range (ISO 8601 format)
     * @return Mono emitting ResponseEntity with quantity response and HATEOAS links
     */
    @GetMapping("/quantity/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.QuantityResponse>>> getQuantityByDateRange(
            @PathVariable UUID warehouseId,
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        log.debug("GET /warehouse/statistic/quantity/{}", warehouseId);

        return statisticService.getQuantityByTypeAndDateRange(warehouseId, type, from, to)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getQuantityByDateRange(warehouseId, type, from, to)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Gets import/export balance for a warehouse.
     * <p>
     * Calculates net balance as (total imports - total exports).
     * Useful for verifying inventory accuracy.
     * 
     * @param warehouseId warehouse unique identifier
     * @return Mono emitting ResponseEntity with balance response and HATEOAS links
     */
    @GetMapping("/balance/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.BalanceResponse>>> getBalance(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/statistic/balance/{}", warehouseId);

        return statisticService.getImportExportBalance(warehouseId)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.BalanceResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getBalance(warehouseId)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    // ==================== WAREHOUSE ALERTS ====================

    /**
     * Gets products below minimum quantity with pagination.
     * <p>
     * Returns warehouses where current quantity < minimum quantity threshold.
     * Useful for restocking alerts.
     * 
     * @param page zero-based page index (default: 0)
     * @param size number of items per page (default: 20)
     * @return Mono emitting ResponseEntity with alert list and HATEOAS links
     */
    @GetMapping("/alerts/below-minimum")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.AlertListResponse>>> getProductsBelowMinimum(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        log.debug("GET /warehouse/statistic/alerts/below-minimum");

        return statisticService.getProductsBelowMinimum(page, size)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getProductsBelowMinimum(page, size)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Gets out of stock products with pagination.
     * <p>
     * Returns warehouses where current quantity = 0.
     * Critical for immediate restocking needs.
     * 
     * @param page zero-based page index (default: 0)
     * @param size number of items per page (default: 20)
     * @return Mono emitting ResponseEntity with alert list and HATEOAS links
     */
    @GetMapping("/alerts/out-of-stock")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.AlertListResponse>>> getOutOfStockProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        log.debug("GET /warehouse/statistic/alerts/out-of-stock");

        return statisticService.getOutOfStockProducts(page, size)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getOutOfStockProducts(page, size)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Gets all warehouse alerts (below minimum + out of stock) with pagination.
     * <p>
     * Merges both alert types and removes duplicates.
     * Provides comprehensive view of inventory issues.
     * 
     * @param page zero-based page index (default: 0)
     * @param size number of items per page (default: 20)
     * @return Mono emitting ResponseEntity with combined alert list and HATEOAS links
     */
    @GetMapping("/alerts")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.AlertListResponse>>> getAllAlerts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        log.debug("GET /warehouse/statistic/alerts");

        return statisticService.getAllWarehouseAlerts(page, size)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getAllAlerts(page, size)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    // ==================== DASHBOARD STATISTICS ====================

    @GetMapping("/dashboard")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.DashboardResponse>>> getDashboard() {
        log.debug("GET /warehouse/statistic/dashboard");

        return statisticService.getDashboardStatistics()
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.DashboardResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getDashboard()).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    @GetMapping("/details/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.WarehouseDetailsResponse>>> getWarehouseDetails(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/statistic/details/{}", warehouseId);

        return statisticService.getWarehouseDetails(warehouseId)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.WarehouseDetailsResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getWarehouseDetails(warehouseId)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    @GetMapping("/time-based/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.TimeBasedStatisticsResponse>>> getTimeBasedStatistics(
            @PathVariable UUID warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        log.debug("GET /warehouse/statistic/time-based/{}", warehouseId);

        return statisticService.getTimeBasedStatistics(warehouseId, from, to)
                .map(response -> {
                    EntityModel<WarehouseStatisticDTO.TimeBasedStatisticsResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseStatisticController.class)
                            .getTimeBasedStatistics(warehouseId, from, to)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }
}