package io.github.lvoxx.srms.warehouse.services;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseCountService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseHistoryRepository warehouseHistoryRepository;

    /**
     * Count all warehouses
     * @param includeDeleted whether to include deleted items
     * @return count of warehouses
     */
    @Cacheable(value = "warehouse:count:all", key = "#includeDeleted")
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
                    "Failed to count warehouses: " + e.getMessage()
                ));
            });
    }

    /**
     * Count warehouses below minimum quantity
     * @return count of warehouses below minimum
     */
    @Cacheable(value = "warehouse:count:below-minimum")
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
                    "Failed to count warehouses below minimum: " + e.getMessage()
                ));
            });
    }

    /**
     * Count out of stock warehouses
     * @return count of out of stock warehouses
     */
    @Cacheable(value = "warehouse:count:out-of-stock")
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
                    "Failed to count out of stock warehouses: " + e.getMessage()
                ));
            });
    }

    /**
     * Count all warehouse history entries
     * @return count of all history entries
     */
    @Cacheable(value = "warehouse:count:history:all")
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
                    "Failed to count history entries: " + e.getMessage()
                ));
            });
    }

    /**
     * Count history entries by warehouse ID
     * @param warehouseId the warehouse ID
     * @return count of history entries for specific warehouse
     */
    @Cacheable(value = "warehouse:count:history:by-warehouse", key = "#warehouseId")
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
                    "Failed to count history for warehouse: " + e.getMessage()
                ));
            });
    }

    /**
     * Count history entries by type
     * @param type the history type (import/export)
     * @return count of history entries by type
     */
    @Cacheable(value = "warehouse:count:history:by-type", key = "#type")
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
                    "Failed to count history by type: " + e.getMessage()
                ));
            });
    }

    /**
     * Count history entries by warehouse ID and type
     * @param warehouseId the warehouse ID
     * @param type the history type
     * @return count of history entries
     */
    @Cacheable(value = "warehouse:count:history:by-warehouse-and-type", key = "#warehouseId + ':' + #type")
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
                    "Failed to count history by warehouse and type: " + e.getMessage()
                ));
            });
    }

    /**
     * Get comprehensive warehouse statistics
     * @return comprehensive statistics
     */
    @Cacheable(value = "warehouse:count:statistics")
    public Mono<WarehouseCountDTO.StatisticsResponse> getWarehouseStatistics() {
        log.debug("Getting comprehensive warehouse statistics");
        
        return Mono.zip(
            warehouseRepository.countAll(false),
            warehouseRepository.countBelowMinimum(),
            warehouseRepository.countOutOfStock(),
            warehouseHistoryRepository.countAll()
        )
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
                "Failed to get warehouse statistics: " + e.getMessage()
            ));
        });
    }

    /**
     * Get warehouse health metrics
     * @return health metrics including percentages
     */
    @Cacheable(value = "warehouse:count:health")
    public Mono<WarehouseCountDTO.HealthMetricsResponse> getWarehouseHealthMetrics() {
        log.debug("Getting warehouse health metrics");
        
        return Mono.zip(
            warehouseRepository.countAll(false),
            warehouseRepository.countBelowMinimum(),
            warehouseRepository.countOutOfStock()
        )
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
                "Failed to get warehouse health metrics: " + e.getMessage()
            ));
        });
    }
}