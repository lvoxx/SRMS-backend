package io.github.lvoxx.srms.warehouse.controllers;

import java.util.UUID;

import org.springframework.hateoas.server.reactive.WebFluxLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
import io.github.lvoxx.srms.warehouse.hateoas.WarehouseCountResource;
import io.github.lvoxx.srms.warehouse.hateoas.WarehouseHealthMetricsResource;
import io.github.lvoxx.srms.warehouse.hateoas.WarehouseStatisticsResource;
import io.github.lvoxx.srms.warehouse.services.WarehouseCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST Controller for warehouse counting and statistics operations.
 * <p>
 * Provides endpoints for counting warehouses by various criteria including
 * total counts, stock status, history entries, and health metrics.
 * All GET endpoints return HATEOAS-compliant responses with navigational links.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/warehouse/count")
@RequiredArgsConstructor
public class WarehouseCountController {

    private final WarehouseCountService countService;

     /**
     * Counts all warehouses in the system.
     * 
     * @param includeDeleted whether to include soft-deleted warehouses in the count
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping
    public Mono<ResponseEntity<WarehouseCountResource>> countAll(
            @RequestParam(defaultValue = "false", required = false) boolean includeDeleted) {
        log.debug("GET /warehouse/count");

        return countService.countAllWarehouses(includeDeleted)
                .flatMap(response -> toCountResource(response, includeDeleted))
                .map(ResponseEntity::ok);
    }

    /**
     * Counts warehouses that are below their minimum quantity threshold.
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/below-minimum")
    public Mono<ResponseEntity<WarehouseCountResource>> countBelowMinimum() {
        log.debug("GET /warehouse/count/below-minimum");

        return countService.countBelowMinimum()
                .flatMap(this::toBelowMinimumResource)
                .map(ResponseEntity::ok);
    }

    /**
     * Counts warehouses that are completely out of stock (quantity = 0).
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/out-of-stock")
    public Mono<ResponseEntity<WarehouseCountResource>> countOutOfStock() {
        log.debug("GET /warehouse/count/out-of-stock");

        return countService.countOutOfStock()
                .flatMap(this::toOutOfStockResource)
                .map(ResponseEntity::ok);
    }

    /**
     * Counts all warehouse history entries across all warehouses.
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<WarehouseCountResource>> countAllHistory() {
        log.debug("GET /warehouse/count/history");

        return countService.countAllHistory()
                .flatMap(this::toAllHistoryResource)
                .map(ResponseEntity::ok);
    }

    /**
     * Counts history entries for a specific warehouse.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/warehouse/{warehouseId}")
    public Mono<ResponseEntity<WarehouseCountResource>> countHistoryByWarehouse(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/count/history/warehouse/{}", warehouseId);

        return countService.countHistoryByWarehouseId(warehouseId)
                .flatMap(response -> toHistoryByWarehouseResource(response, warehouseId))
                .map(ResponseEntity::ok);
    }

    /**
     * Counts history entries by transaction type (import or export).
     * 
     * @param type transaction type (import or export)
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/type/{type}")
    public Mono<ResponseEntity<WarehouseCountResource>> countHistoryByType(
            @PathVariable String type) {
        log.debug("GET /warehouse/count/history/type/{}", type);

        return countService.countHistoryByType(type)
                .flatMap(response -> toHistoryByTypeResource(response, type))
                .map(ResponseEntity::ok);
    }

    /**
     * Counts history entries for a specific warehouse and transaction type.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @param type        transaction type (import or export)
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/warehouse/{warehouseId}/type/{type}")
    public Mono<ResponseEntity<WarehouseCountResource>> countHistoryByWarehouseAndType(
            @PathVariable UUID warehouseId, @PathVariable String type) {
        log.debug("GET /warehouse/count/history/warehouse/{}/type/{}", warehouseId, type);

        return countService.countHistoryByWarehouseIdAndType(warehouseId, type)
                .flatMap(response -> toHistoryByWarehouseAndTypeResource(response, warehouseId, type))
                .map(ResponseEntity::ok);
    }

    /**
     * Gets comprehensive warehouse statistics.
     * <p>
     * Aggregates multiple metrics including total warehouses, in-stock count,
     * out-of-stock count, below-minimum count, and total history entries.
     * 
     * @return Mono emitting ResponseEntity with statistics response and HATEOAS links
     */
    @GetMapping("/statistics")
    public Mono<ResponseEntity<WarehouseStatisticsResource>> getStatistics() {
        log.debug("GET /warehouse/count/statistics");

        return countService.getWarehouseStatistics()
                .flatMap(this::toStatisticsResource)
                .map(ResponseEntity::ok);
    }

    /**
     * Gets warehouse health metrics with percentage indicators.
     * <p>
     * Provides health analysis including below-minimum percentage,
     * out-of-stock percentage, and overall healthy percentage.
     * 
     * @return Mono emitting ResponseEntity with health metrics and HATEOAS links
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<WarehouseHealthMetricsResource>> getHealthMetrics() {
        log.debug("GET /warehouse/count/health");

        return countService.getWarehouseHealthMetrics()
                .flatMap(this::toHealthMetricsResource)
                .map(ResponseEntity::ok);
    }

    // ==================== HATEOAS Resource Converters ====================

    private Mono<WarehouseCountResource> toCountResource(WarehouseCountDTO.CountResponse dto, boolean includeDeleted) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countAll(includeDeleted))
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getStatistics())
                        .withRel("statistics")
                        .toMono()
                        .map(statsLink -> {
                            res.add(statsLink);
                            return res;
                        }))
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getHealthMetrics())
                        .withRel("health")
                        .toMono()
                        .map(healthLink -> {
                            res.add(healthLink);
                            return res;
                        }));
    }

    private Mono<WarehouseCountResource> toBelowMinimumResource(WarehouseCountDTO.CountResponse dto) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countBelowMinimum())
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countAll(false))
                        .withRel("all")
                        .toMono()
                        .map(allLink -> {
                            res.add(allLink);
                            return res;
                        }));
    }

    private Mono<WarehouseCountResource> toOutOfStockResource(WarehouseCountDTO.CountResponse dto) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countOutOfStock())
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countAll(false))
                        .withRel("all")
                        .toMono()
                        .map(allLink -> {
                            res.add(allLink);
                            return res;
                        }));
    }

    private Mono<WarehouseCountResource> toAllHistoryResource(WarehouseCountDTO.CountResponse dto) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countAllHistory())
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                });
    }

    private Mono<WarehouseCountResource> toHistoryByWarehouseResource(WarehouseCountDTO.CountResponse dto, UUID warehouseId) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countHistoryByWarehouse(warehouseId))
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countAllHistory())
                        .withRel("all-history")
                        .toMono()
                        .map(allHistoryLink -> {
                            res.add(allHistoryLink);
                            return res;
                        }));
    }

    private Mono<WarehouseCountResource> toHistoryByTypeResource(WarehouseCountDTO.CountResponse dto, String type) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).countHistoryByType(type))
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                });
    }

    private Mono<WarehouseCountResource> toHistoryByWarehouseAndTypeResource(
            WarehouseCountDTO.CountResponse dto, UUID warehouseId, String type) {
        WarehouseCountResource resource = new WarehouseCountResource();
        resource.setCount(dto.getCount());
        resource.setDescription(dto.getDescription());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class)
                        .countHistoryByWarehouseAndType(warehouseId, type))
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                });
    }

    private Mono<WarehouseStatisticsResource> toStatisticsResource(WarehouseCountDTO.StatisticsResponse dto) {
        WarehouseStatisticsResource resource = new WarehouseStatisticsResource();
        resource.setTotalWarehouses(dto.getTotalWarehouses());
        resource.setInStock(dto.getInStock());
        resource.setOutOfStock(dto.getOutOfStock());
        resource.setBelowMinimum(dto.getBelowMinimum());
        resource.setTotalHistoryEntries(dto.getTotalHistoryEntries());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getStatistics())
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getHealthMetrics())
                        .withRel("health")
                        .toMono()
                        .map(healthLink -> {
                            res.add(healthLink);
                            return res;
                        }));
    }

    private Mono<WarehouseHealthMetricsResource> toHealthMetricsResource(WarehouseCountDTO.HealthMetricsResponse dto) {
        WarehouseHealthMetricsResource resource = new WarehouseHealthMetricsResource();
        resource.setTotalWarehouses(dto.getTotalWarehouses());
        resource.setBelowMinimum(dto.getBelowMinimum());
        resource.setOutOfStock(dto.getOutOfStock());
        resource.setBelowMinimumPercentage(dto.getBelowMinimumPercentage());
        resource.setOutOfStockPercentage(dto.getOutOfStockPercentage());
        resource.setHealthyPercentage(dto.getHealthyPercentage());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getHealthMetrics())
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(WarehouseCountController.class).getStatistics())
                        .withRel("statistics")
                        .toMono()
                        .map(statsLink -> {
                            res.add(statsLink);
                            return res;
                        }));
    }
}
