package io.github.lvoxx.srms.warehouse.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.reactive.WebFluxLinkBuilder;
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
                .flatMap(response -> addImportLinks(response, warehouseId))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addExportLinks(response, warehouseId))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addQuantityByDateRangeLinks(response, warehouseId, type, from, to))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addBalanceLinks(response, warehouseId))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addBelowMinimumLinks(response, page, size))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addOutOfStockLinks(response, page, size))
                .map(ResponseEntity::ok);
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
                .flatMap(response -> addAllAlertsLinks(response, page, size))
                .map(ResponseEntity::ok);
    }

    // ==================== DASHBOARD STATISTICS ====================

    @GetMapping("/dashboard")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.DashboardResponse>>> getDashboard() {
        log.debug("GET /warehouse/statistic/dashboard");

        return statisticService.getDashboardStatistics()
                .flatMap(this::addDashboardLinks)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/details/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.WarehouseDetailsResponse>>> getWarehouseDetails(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/statistic/details/{}", warehouseId);

        return statisticService.getWarehouseDetails(warehouseId)
                .flatMap(response -> addWarehouseDetailsLinks(response, warehouseId))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/time-based/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseStatisticDTO.TimeBasedStatisticsResponse>>> getTimeBasedStatistics(
            @PathVariable UUID warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        log.debug("GET /warehouse/statistic/time-based/{}", warehouseId);

        return statisticService.getTimeBasedStatistics(warehouseId, from, to)
                .flatMap(response -> addTimeBasedStatisticsLinks(response, warehouseId, from, to))
                .map(ResponseEntity::ok);
    }

    // ==================== PRIVATE HATEOAS LINK BUILDERS ====================

    private Mono<EntityModel<WarehouseStatisticDTO.QuantityResponse>> addImportLinks(
            WarehouseStatisticDTO.QuantityResponse response, UUID warehouseId) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getTotalImport(warehouseId))
                .withSelfRel()
                .toMono()
                .zipWith(WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                                .getTotalExport(warehouseId))
                        .withRel("export")
                        .toMono())
                .zipWith(WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                                .getBalance(warehouseId))
                        .withRel("balance")
                        .toMono())
                .map(tuple -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(tuple.getT1().getT1());
                    resource.add(tuple.getT1().getT2());
                    resource.add(tuple.getT2());
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.QuantityResponse>> addExportLinks(
            WarehouseStatisticDTO.QuantityResponse response, UUID warehouseId) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getTotalExport(warehouseId))
                .withSelfRel()
                .toMono()
                .zipWith(WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                                .getTotalImport(warehouseId))
                        .withRel("import")
                        .toMono())
                .map(tuple -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(tuple.getT1());
                    resource.add(tuple.getT2());
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.QuantityResponse>> addQuantityByDateRangeLinks(
            WarehouseStatisticDTO.QuantityResponse response, UUID warehouseId, 
            String type, OffsetDateTime from, OffsetDateTime to) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getQuantityByDateRange(warehouseId, type, from, to))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.QuantityResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.BalanceResponse>> addBalanceLinks(
            WarehouseStatisticDTO.BalanceResponse response, UUID warehouseId) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getBalance(warehouseId))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.BalanceResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.AlertListResponse>> addBelowMinimumLinks(
            WarehouseStatisticDTO.AlertListResponse response, int page, int size) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getProductsBelowMinimum(page, size))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.AlertListResponse>> addOutOfStockLinks(
            WarehouseStatisticDTO.AlertListResponse response, int page, int size) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getOutOfStockProducts(page, size))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.AlertListResponse>> addAllAlertsLinks(
            WarehouseStatisticDTO.AlertListResponse response, int page, int size) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getAllAlerts(page, size))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.AlertListResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.DashboardResponse>> addDashboardLinks(
            WarehouseStatisticDTO.DashboardResponse response) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getDashboard())
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.DashboardResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.WarehouseDetailsResponse>> addWarehouseDetailsLinks(
            WarehouseStatisticDTO.WarehouseDetailsResponse response, UUID warehouseId) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getWarehouseDetails(warehouseId))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.WarehouseDetailsResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }

    private Mono<EntityModel<WarehouseStatisticDTO.TimeBasedStatisticsResponse>> addTimeBasedStatisticsLinks(
            WarehouseStatisticDTO.TimeBasedStatisticsResponse response, UUID warehouseId,
            OffsetDateTime from, OffsetDateTime to) {
        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseStatisticController.class)
                        .getTimeBasedStatistics(warehouseId, from, to))
                .withSelfRel()
                .toMono()
                .map(link -> {
                    EntityModel<WarehouseStatisticDTO.TimeBasedStatisticsResponse> resource = EntityModel.of(response);
                    resource.add(link);
                    return resource;
                });
    }
}