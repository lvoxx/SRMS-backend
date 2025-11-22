package io.github.lvoxx.srms.warehouse.services;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.lvoxx.srms.common.cache.WarehouseCacheNames;
import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service providing counting operations for warehouse entities.
 * <p>
 * This service handles all counting-related operations including total
 * warehouse counts,
 * stock status counts (below minimum, out of stock), history entry counts,
 * comprehensive
 * statistics aggregation, and health metrics calculation.
 * <p>
 * All operations are cached using Spring's caching abstraction to optimize
 * performance.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseCountService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseHistoryRepository warehouseHistoryRepository;

    /**
     * Counts all warehouses in the system.
     * 
     * @param includeDeleted true to include soft-deleted warehouses, false for
     *                       active only
     * @return Mono emitting count response with total number and description
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_ALL, key = "#includeDeleted")
    public Mono<WarehouseCountDTO.CountResponse> countAllWarehouses(boolean includeDeleted) {
        log.debug("Counting all warehouses with includeDeleted={}", includeDeleted);

        return warehouseRepository.countAll(includeDeleted)
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("Total warehouses")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting all warehouses: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count warehouses: " + e.getMessage()));
                });
    }

    /**
     * Counts warehouses that have quantities below their defined minimum threshold.
     * 
     * @return Mono emitting count of warehouses below minimum
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_BELOW_MINIMUM)
    public Mono<WarehouseCountDTO.CountResponse> countBelowMinimum() {
        log.debug("Counting warehouses below minimum quantity");

        return warehouseRepository.countBelowMinimum()
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("Warehouses below minimum quantity")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting warehouses below minimum: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count warehouses below minimum: " + e.getMessage()));
                });
    }

    /**
     * Counts warehouses that are completely out of stock (quantity = 0).
     * 
     * @return Mono emitting count of out-of-stock warehouses
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_OUT_OF_STOCK)
    public Mono<WarehouseCountDTO.CountResponse> countOutOfStock() {
        log.debug("Counting out of stock warehouses");

        return warehouseRepository.countOutOfStock()
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("Out of stock warehouses")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting out of stock warehouses: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count out of stock warehouses: " + e.getMessage()));
                });
    }

    /**
     * Counts all warehouse history entries across all warehouses.
     * 
     * @return Mono emitting total count of history entries
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_HISTORY_ALL)
    public Mono<WarehouseCountDTO.CountResponse> countAllHistory() {
        log.debug("Counting all warehouse history entries");

        return warehouseHistoryRepository.countAll()
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("Total history entries")
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting all history entries: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count history entries: " + e.getMessage()));
                });
    }

    /**
     * Counts history entries for a specific warehouse.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting count of history entries for the warehouse
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_HISTORY_BY_WAREHOUSE, key = "#warehouseId")
    public Mono<WarehouseCountDTO.CountResponse> countHistoryByWarehouseId(UUID warehouseId) {
        log.debug("Counting history entries for warehouse: {}", warehouseId);

        return warehouseHistoryRepository.countByWarehouseId(warehouseId)
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("History entries for warehouse " + warehouseId)
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting history for warehouse {}: {}", warehouseId, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count history for warehouse: " + e.getMessage()));
                });
    }

    /**
     * Counts history entries by transaction type (import or export).
     * 
     * @param type transaction type ("import" or "export")
     * @return Mono emitting count of entries for specified type
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_HISTORY_BY_TYPE, key = "#type")
    public Mono<WarehouseCountDTO.CountResponse> countHistoryByType(String type) {
        log.debug("Counting history entries by type: {}", type);

        return warehouseHistoryRepository.countByType(type)
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description("History entries of type: " + type)
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting history by type {}: {}", type, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count history by type: " + e.getMessage()));
                });
    }

    /**
     * Counts history entries for a specific warehouse and transaction type.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @param type        transaction type ("import" or "export")
     * @return Mono emitting count of matching entries
     * @throws InternalServerException if database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_HISTORY_BY_WAREHOUSE_AND_TYPE, key = "#warehouseId + ':' + #type")
    public Mono<WarehouseCountDTO.CountResponse> countHistoryByWarehouseIdAndType(
            UUID warehouseId, String type) {
        log.debug("Counting history entries for warehouse {} and type {}", warehouseId, type);

        return warehouseHistoryRepository.countByWarehouseIdAndType(warehouseId, type)
                .map(count -> WarehouseCountDTO.CountResponse.builder()
                        .count(count)
                        .description(String.format("History entries for warehouse %s of type %s",
                                warehouseId, type))
                        .build())
                .onErrorResume(e -> {
                    log.error("Error counting history for warehouse {} and type {}: {}",
                            warehouseId, type, e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to count history by warehouse and type: " + e.getMessage()));
                });
    }

    /**
     * Retrieves comprehensive warehouse statistics in a single aggregated response.
     * <p>
     * Combines multiple count operations in parallel using Mono.zip for optimal
     * performance.
     * 
     * @return Mono emitting comprehensive statistics (total, below minimum, out of
     *         stock, history entries, in stock)
     * @throws InternalServerException if any database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_STATISTICS)
    public Mono<WarehouseCountDTO.StatisticsResponse> getWarehouseStatistics() {
        log.debug("Getting comprehensive warehouse statistics");

        return Mono.zip(
                warehouseRepository.countAll(false),
                warehouseRepository.countBelowMinimum(),
                warehouseRepository.countOutOfStock(),
                warehouseHistoryRepository.countAll())
                .map(tuple -> WarehouseCountDTO.StatisticsResponse.builder()
                        .totalWarehouses(tuple.getT1())
                        .belowMinimum(tuple.getT2())
                        .outOfStock(tuple.getT3())
                        .totalHistoryEntries(tuple.getT4())
                        .inStock(tuple.getT1() - tuple.getT3())
                        .build())
                .onErrorResume(e -> {
                    log.error("Error getting warehouse statistics: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get warehouse statistics: " + e.getMessage()));
                });
    }

    /**
     * Calculates warehouse health metrics with percentage indicators.
     * <p>
     * Provides percentages for warehouses below minimum, out of stock, and overall
     * health.
     * Protects against division by zero when total is 0.
     * 
     * @return Mono emitting health metrics with counts and percentages
     * @throws InternalServerException if any database operation fails
     */
    @Cacheable(value = WarehouseCacheNames.COUNT_HEALTH)
    public Mono<WarehouseCountDTO.HealthMetricsResponse> getWarehouseHealthMetrics() {
        log.debug("Getting warehouse health metrics");

        return Mono.zip(
                warehouseRepository.countAll(false),
                warehouseRepository.countBelowMinimum(),
                warehouseRepository.countOutOfStock())
                .map(tuple -> {
                    Long total = tuple.getT1();
                    Long belowMin = tuple.getT2();
                    Long outOfStock = tuple.getT3();

                    double belowMinPercentage = total > 0 ? (belowMin * 100.0 / total) : 0.0;
                    double outOfStockPercentage = total > 0 ? (outOfStock * 100.0 / total) : 0.0;
                    double healthyPercentage = 100.0 - belowMinPercentage;

                    return WarehouseCountDTO.HealthMetricsResponse.builder()
                            .totalWarehouses(total)
                            .belowMinimum(belowMin)
                            .outOfStock(outOfStock)
                            .belowMinimumPercentage(belowMinPercentage)
                            .outOfStockPercentage(outOfStockPercentage)
                            .healthyPercentage(healthyPercentage)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error getting warehouse health metrics: {}", e.getMessage(), e);
                    return Mono.error(new InternalServerException(
                            "Failed to get warehouse health metrics: " + e.getMessage()));
                });
    }
}