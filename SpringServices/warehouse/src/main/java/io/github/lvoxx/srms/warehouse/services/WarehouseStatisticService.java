package io.github.lvoxx.srms.warehouse.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.lvoxx.srms.common.cache.WarehouseCacheNames;
import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import io.github.lvoxx.srms.warehouse.mapper.WarehouseMapper;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service providing statistical analysis and reporting for warehouse data.
 * <p>
 * Handles import/export statistics, warehouse alerts, dashboard metrics,
 * and real-time data streaming for WebSocket connections.
 * All statistical queries are cached for optimal performance.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseStatisticService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseHistoryRepository warehouseHistoryRepository;
    private final WarehouseMapper warehouseMapper;

    // ==================== IMPORT/EXPORT STATISTICS ====================

    /**
     * Gets total import quantity for a warehouse.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting quantity response with total imports
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_TOTAL_IMPORT, key = "#warehouseId")
    public Mono<WarehouseStatisticDTO.QuantityResponse> getTotalImportQuantity(UUID warehouseId) {
        log.debug("Getting total import quantity for warehouse: {}", warehouseId);

        return warehouseHistoryRepository.getTotalImportQuantity(warehouseId)
                .map(quantity -> WarehouseStatisticDTO.QuantityResponse.builder()
                        .warehouseId(warehouseId)
                        .quantity(quantity)
                        .type("IMPORT")
                        .description("Total imported quantity")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting total import quantity for warehouse {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get total import quantity: " + e.getMessage()));
                });
    }

    /**
     * Gets total export quantity for a warehouse.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting quantity response with total exports
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_TOTAL_EXPORT, key = "#warehouseId")
    public Mono<WarehouseStatisticDTO.QuantityResponse> getTotalExportQuantity(UUID warehouseId) {
        log.debug("Getting total export quantity for warehouse: {}", warehouseId);

        return warehouseHistoryRepository.getTotalExportQuantity(warehouseId)
                .map(quantity -> WarehouseStatisticDTO.QuantityResponse.builder()
                        .warehouseId(warehouseId)
                        .quantity(quantity)
                        .type("EXPORT")
                        .description("Total exported quantity")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting total export quantity for warehouse {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get total export quantity: " + e.getMessage()));
                });
    }

    /**
     * Gets quantity by type and date range.
     * <p>
     * Useful for analyzing import/export patterns over specific time periods.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @param type        transaction type ("import" or "export")
     * @param from        start date of range
     * @param to          end date of range
     * @return Mono emitting quantity response for specified period
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_QUANTITY_BY_DATE_RANGE, key = "#warehouseId + ':' + #type + ':' + #from + ':' + #to")
    public Mono<WarehouseStatisticDTO.QuantityResponse> getQuantityByTypeAndDateRange(
            UUID warehouseId, String type, OffsetDateTime from, OffsetDateTime to) {
        log.debug("Getting {} quantity for warehouse {} from {} to {}",
                type, warehouseId, from, to);

        return warehouseHistoryRepository.getQuantityByTypeAndDateRange(warehouseId, type, from, to)
                .map(quantity -> WarehouseStatisticDTO.QuantityResponse.builder()
                        .warehouseId(warehouseId)
                        .quantity(quantity)
                        .type(type.toUpperCase())
                        .fromDate(from)
                        .toDate(to)
                        .description(String.format("%s quantity from %s to %s",
                                type.toUpperCase(), from, to))
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting quantity by type {} and date range for warehouse {}: {}",
                            type, warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get quantity by type and date range: " + e.getMessage()));
                });
    }

    /**
     * Gets import/export balance for a warehouse.
     * <p>
     * Calculates net balance as (total imports - total exports).
     * Useful for verifying inventory accuracy.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting balance response with imports, exports, and net balance
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_BALANCE, key = "#warehouseId")
    public Mono<WarehouseStatisticDTO.BalanceResponse> getImportExportBalance(UUID warehouseId) {
        log.debug("Getting import/export balance for warehouse: {}", warehouseId);

        return Mono.zip(
                warehouseHistoryRepository.getTotalImportQuantity(warehouseId),
                warehouseHistoryRepository.getTotalExportQuantity(warehouseId))
                .map(tuple -> {
                    Long totalImport = tuple.getT1();
                    Long totalExport = tuple.getT2();
                    Long balance = totalImport - totalExport;

                    return WarehouseStatisticDTO.BalanceResponse.builder()
                            .warehouseId(warehouseId)
                            .totalImport(totalImport)
                            .totalExport(totalExport)
                            .balance(balance)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error getting balance for warehouse {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get import/export balance: " + e.getMessage()));
                });
    }

    // ==================== WAREHOUSE ALERTS ====================

    /**
     * Gets products below minimum quantity with pagination.
     * <p>
     * Returns warehouses where current quantity < minimum quantity threshold.
     * Useful for restocking alerts.
     * 
     * @param page zero-based page index
     * @param size number of items per page
     * @return Mono emitting alert list with items and total count
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_BELOW_MINIMUM, key = "#page + ':' + #size")
    public Mono<WarehouseStatisticDTO.AlertListResponse> getProductsBelowMinimum(
            int page, int size) {
        log.debug("Getting products below minimum - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return warehouseRepository.findProductsBelowMinimum(pageable)
                .map(this::mapToAlertItem)
                .collectList()
                .zipWith(warehouseRepository.countBelowMinimum())
                .map(tuple -> WarehouseStatisticDTO.AlertListResponse.builder()
                        .items(tuple.getT1())
                        .totalItems(tuple.getT2())
                        .page(page)
                        .size(size)
                        .alertType("BELOW_MINIMUM")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting products below minimum: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get products below minimum: " + e.getMessage()));
                });
    }

    /**
     * Gets out of stock products with pagination.
     * <p>
     * Returns warehouses where current quantity = 0.
     * Critical for immediate restocking needs.
     * 
     * @param page zero-based page index
     * @param size number of items per page
     * @return Mono emitting alert list with items and total count
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_OUT_OF_STOCK, key = "#page + ':' + #size")
    public Mono<WarehouseStatisticDTO.AlertListResponse> getOutOfStockProducts(
            int page, int size) {
        log.debug("Getting out of stock products - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return warehouseRepository.findOutOfStock(pageable)
                .map(this::mapToAlertItem)
                .collectList()
                .zipWith(warehouseRepository.countOutOfStock())
                .map(tuple -> WarehouseStatisticDTO.AlertListResponse.builder()
                        .items(tuple.getT1())
                        .totalItems(tuple.getT2())
                        .page(page)
                        .size(size)
                        .alertType("OUT_OF_STOCK")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting out of stock products: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get out of stock products: " + e.getMessage()));
                });
    }

    /**
     * Gets all warehouse alerts (below minimum + out of stock) with pagination.
     * <p>
     * Merges both alert types and removes duplicates.
     * Provides comprehensive view of inventory issues.
     * 
     * @param page zero-based page index
     * @param size number of items per page
     * @return Mono emitting combined alert list
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_ALL_ALERTS, key = "#page + ':' + #size")
    public Mono<WarehouseStatisticDTO.AlertListResponse> getAllWarehouseAlerts(
            int page, int size) {
        log.debug("Getting all warehouse alerts - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return Flux.merge(
                warehouseRepository.findProductsBelowMinimum(pageable)
                        .map(this::mapToAlertItem),
                warehouseRepository.findOutOfStock(pageable)
                        .map(this::mapToAlertItem))
                .distinct(WarehouseStatisticDTO.AlertItem::getId)
                .collectList()
                .zipWith(
                        Mono.zip(
                                warehouseRepository.countBelowMinimum(),
                                warehouseRepository.countOutOfStock()).map(tuple -> tuple.getT1() + tuple.getT2()))
                .map(tuple -> WarehouseStatisticDTO.AlertListResponse.builder()
                        .items(tuple.getT1())
                        .totalItems(tuple.getT2())
                        .page(page)
                        .size(size)
                        .alertType("ALL_ALERTS")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting all warehouse alerts: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get all warehouse alerts: " + e.getMessage()));
                });
    }

    // ==================== DASHBOARD STATISTICS ====================

    /**
     * Gets comprehensive dashboard statistics.
     * <p>
     * Aggregates multiple metrics in parallel:
     * - Total warehouses
     * - Healthy warehouses (not below minimum)
     * - Warehouses below minimum
     * - Out of stock warehouses
     * - Health percentage
     * - Total transactions
     * - Import/export transaction counts
     * 
     * @return Mono emitting dashboard response with all metrics
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_DASHBOARD, key = "'comprehensive'")
    public Mono<WarehouseStatisticDTO.DashboardResponse> getDashboardStatistics() {
        log.debug("Getting comprehensive dashboard statistics");

        return Mono.zip(
                warehouseRepository.countAll(false),
                warehouseRepository.countBelowMinimum(),
                warehouseRepository.countOutOfStock(),
                warehouseHistoryRepository.countAll(),
                warehouseHistoryRepository.countByType("import"),
                warehouseHistoryRepository.countByType("export"))
                .map(tuple -> {
                    Long totalWarehouses = tuple.getT1();
                    Long belowMinimum = tuple.getT2();
                    Long outOfStock = tuple.getT3();
                    Long totalHistory = tuple.getT4();
                    Long totalImports = tuple.getT5();
                    Long totalExports = tuple.getT6();

                    Long healthyWarehouses = totalWarehouses - belowMinimum;
                    double healthPercentage = totalWarehouses > 0
                            ? (healthyWarehouses * 100.0 / totalWarehouses)
                            : 100.0;

                    return WarehouseStatisticDTO.DashboardResponse.builder()
                            .totalWarehouses(totalWarehouses)
                            .healthyWarehouses(healthyWarehouses)
                            .belowMinimum(belowMinimum)
                            .outOfStock(outOfStock)
                            .healthPercentage(healthPercentage)
                            .totalTransactions(totalHistory)
                            .totalImportTransactions(totalImports)
                            .totalExportTransactions(totalExports)
                            .timestamp(OffsetDateTime.now())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error getting dashboard statistics: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get dashboard statistics: " + e.getMessage()));
                });
    }

    /**
     * Gets warehouse details with statistics.
     * <p>
     * Combines warehouse info with transaction statistics:
     * - Warehouse basic info
     * - Total imports/exports
     * - Balance
     * - Transaction count
     * - Alert flags (below minimum, out of stock)
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting detailed warehouse statistics
     * @throws NotFoundException       if warehouse not found
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_DETAILS, key = "#warehouseId")
    public Mono<WarehouseStatisticDTO.WarehouseDetailsResponse> getWarehouseDetails(
            UUID warehouseId) {
        log.debug("Getting details for warehouse: {}", warehouseId);

        return warehouseRepository.findById(warehouseId, false)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Warehouse not found with id: " + warehouseId)))
                .flatMap(warehouse -> Mono.zip(
                        Mono.just(warehouse),
                        warehouseHistoryRepository.getTotalImportQuantity(warehouseId),
                        warehouseHistoryRepository.getTotalExportQuantity(warehouseId),
                        warehouseHistoryRepository.countByWarehouseId(warehouseId)))
                .map(tuple -> {
                    Warehouse warehouse = tuple.getT1();
                    Long totalImport = tuple.getT2();
                    Long totalExport = tuple.getT3();
                    Long transactionCount = tuple.getT4();
                    Long balance = totalImport - totalExport;

                    return WarehouseStatisticDTO.WarehouseDetailsResponse.builder()
                            .warehouse(warehouseMapper.toResponse(warehouse))
                            .totalImport(totalImport)
                            .totalExport(totalExport)
                            .balance(balance)
                            .transactionCount(transactionCount)
                            .isBelowMinimum(warehouse.isBelowMinimum())
                            .isOutOfStock(!warehouse.isInStock())
                            .timestamp(OffsetDateTime.now())
                            .build();
                })
                .onErrorResume(e -> {
                    if (e instanceof NotFoundException)
                        return Mono.error(e);
                    log.error("Error getting warehouse details for {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get warehouse details: " + e.getMessage()));
                });
    }

    /**
     * Gets time-based statistics for date range.
     * <p>
     * Analyzes import/export activity within specified time period.
     * Calculates net change as (imports - exports).
     * 
     * @param warehouseId unique identifier of the warehouse
     * @param from        start date of analysis period
     * @param to          end date of analysis period
     * @return Mono emitting time-based statistics with import, export, and net
     *         change
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.STATS_TIME_BASED, key = "#warehouseId + ':' + #from + ':' + #to")
    public Mono<WarehouseStatisticDTO.TimeBasedStatisticsResponse> getTimeBasedStatistics(
            UUID warehouseId, OffsetDateTime from, OffsetDateTime to) {
        log.debug("Getting time-based statistics for warehouse {} from {} to {}",
                warehouseId, from, to);

        return Mono.zip(
                warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                        warehouseId, "import", from, to),
                warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                        warehouseId, "export", from, to))
                .map(tuple -> {
                    Long importQuantity = tuple.getT1();
                    Long exportQuantity = tuple.getT2();
                    Long netChange = importQuantity - exportQuantity;

                    return WarehouseStatisticDTO.TimeBasedStatisticsResponse.builder()
                            .warehouseId(warehouseId)
                            .fromDate(from)
                            .toDate(to)
                            .importQuantity(importQuantity)
                            .exportQuantity(exportQuantity)
                            .netChange(netChange)
                            .timestamp(OffsetDateTime.now())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error getting time-based statistics for warehouse {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get time-based statistics: " + e.getMessage()));
                });
    }

    // ==================== REAL-TIME STATISTICS (For WebSocket)
    // ====================

    /**
     * Gets real-time statistics stream for WebSocket dashboard.
     * <p>
     * Emits dashboard statistics every 5 seconds.
     * Continues until subscription is cancelled.
     * 
     * @return Flux streaming dashboard statistics at 5-second intervals
     */
    public Flux<WarehouseStatisticDTO.DashboardResponse> streamDashboardStatistics() {
        log.debug("Starting dashboard statistics stream");

        return Flux.interval(java.time.Duration.ofSeconds(5))
                .flatMap(tick -> getDashboardStatistics())
                .onErrorResume(e -> {
                    log.error("Error in dashboard statistics stream: {}", e.getMessage(), e);
                    return Flux.empty();
                });
    }

    /**
     * Gets real-time warehouse details stream.
     * <p>
     * Emits warehouse details every 5 seconds.
     * Continues until subscription is cancelled.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Flux streaming warehouse details at 5-second intervals
     * @throws NotFoundException if warehouse not found
     */
    public Flux<WarehouseStatisticDTO.WarehouseDetailsResponse> streamWarehouseDetails(
            UUID warehouseId) {
        log.debug("Starting warehouse details stream for: {}", warehouseId);

        return Flux.interval(java.time.Duration.ofSeconds(5))
                .flatMap(tick -> getWarehouseDetails(warehouseId))
                .onErrorResume(e -> {
                    if (e instanceof NotFoundException)
                        return Mono.error(e);
                    log.error("Error in warehouse details stream for {}: {}",
                            warehouseId, e.getMessage(), e);
                    return Flux.empty();
                });
    }

    /**
     * Gets real-time alerts stream.
     * <p>
     * Emits warehouse alerts every 10 seconds.
     * Continues until subscription is cancelled.
     * 
     * @return Flux streaming warehouse alerts at 10-second intervals
     */
    public Flux<WarehouseStatisticDTO.AlertListResponse> streamWarehouseAlerts() {
        log.debug("Starting warehouse alerts stream");

        return Flux.interval(java.time.Duration.ofSeconds(10))
                .flatMap(tick -> getAllWarehouseAlerts(0, 20))
                .onErrorResume(e -> {
                    log.error("Error in warehouse alerts stream: {}", e.getMessage(), e);
                    return Flux.empty();
                });
    }

    // ==================== HELPER METHODS ====================

    /**
     * Maps warehouse entity to alert item with severity calculation.
     * <p>
     * Determines severity level:
     * - CRITICAL: Out of stock (quantity = 0)
     * - WARNING: Below minimum (quantity < minQuantity)
     * - INFO: Normal stock level
     * 
     * Calculates deficit as (minQuantity - currentQuantity).
     * 
     * @param warehouse warehouse entity to map
     * @return AlertItem with severity, message, and deficit calculation
     */
    private WarehouseStatisticDTO.AlertItem mapToAlertItem(Warehouse warehouse) {
        String severity;
        String message;

        if (!warehouse.isInStock()) {
            severity = "CRITICAL";
            message = "Product is out of stock";
        } else if (warehouse.isBelowMinimum()) {
            int deficit = warehouse.getMinQuantity() - warehouse.getQuantity();
            severity = "WARNING";
            message = String.format("Below minimum by %d units", deficit);
        } else {
            severity = "INFO";
            message = "Stock level normal";
        }

        return WarehouseStatisticDTO.AlertItem.builder()
                .id(warehouse.getId())
                .productName(warehouse.getProductName())
                .currentQuantity(warehouse.getQuantity())
                .minQuantity(warehouse.getMinQuantity())
                .deficit(warehouse.getMinQuantity() - warehouse.getQuantity())
                .severity(severity)
                .message(message)
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
}
