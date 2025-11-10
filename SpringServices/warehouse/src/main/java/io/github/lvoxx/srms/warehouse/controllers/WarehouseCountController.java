package io.github.lvoxx.srms.warehouse.controllers;

import java.util.UUID;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
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
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countAll(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.debug("GET /warehouse/count");

        return countService.countAllWarehouses(includeDeleted)
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countAll(includeDeleted)).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getStatistics()).withRel("statistics"));
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getHealthMetrics()).withRel("health"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts warehouses that are below their minimum quantity threshold.
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/below-minimum")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countBelowMinimum() {
        log.debug("GET /warehouse/count/below-minimum");

        return countService.countBelowMinimum()
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countBelowMinimum()).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countAll(false)).withRel("all"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts warehouses that are completely out of stock (quantity = 0).
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/out-of-stock")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countOutOfStock() {
        log.debug("GET /warehouse/count/out-of-stock");

        return countService.countOutOfStock()
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countOutOfStock()).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countAll(false)).withRel("all"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts all warehouse history entries across all warehouses.
     * 
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countAllHistory() {
        log.debug("GET /warehouse/count/history");

        return countService.countAllHistory()
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countAllHistory()).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts history entries for a specific warehouse.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/warehouse/{warehouseId}")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countHistoryByWarehouse(
            @PathVariable UUID warehouseId) {
        log.debug("GET /warehouse/count/history/warehouse/{}", warehouseId);

        return countService.countHistoryByWarehouseId(warehouseId)
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countHistoryByWarehouse(warehouseId)).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countAllHistory()).withRel("all-history"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts history entries by transaction type (import or export).
     * 
     * @param type transaction type (import or export)
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/type/{type}")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countHistoryByType(
            @PathVariable String type) {
        log.debug("GET /warehouse/count/history/type/{}", type);

        return countService.countHistoryByType(type)
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countHistoryByType(type)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Counts history entries for a specific warehouse and transaction type.
     * 
     * @param warehouseId unique identifier of the warehouse
     * @param type        transaction type (import or export)
     * @return Mono emitting ResponseEntity with count response and HATEOAS links
     */
    @GetMapping("/history/warehouse/{warehouseId}/type/{type}")
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.CountResponse>>> countHistoryByWarehouseAndType(
            @PathVariable UUID warehouseId, @PathVariable String type) {
        log.debug("GET /warehouse/count/history/warehouse/{}/type/{}", warehouseId, type);

        return countService.countHistoryByWarehouseIdAndType(warehouseId, type)
                .map(response -> {
                    EntityModel<WarehouseCountDTO.CountResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .countHistoryByWarehouseAndType(warehouseId, type)).withSelfRel());
                    return ResponseEntity.ok(resource);
                });
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
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.StatisticsResponse>>> getStatistics() {
        log.debug("GET /warehouse/count/statistics");

        return countService.getWarehouseStatistics()
                .map(response -> {
                    EntityModel<WarehouseCountDTO.StatisticsResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getStatistics()).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getHealthMetrics()).withRel("health"));
                    return ResponseEntity.ok(resource);
                });
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
    public Mono<ResponseEntity<EntityModel<WarehouseCountDTO.HealthMetricsResponse>>> getHealthMetrics() {
        log.debug("GET /warehouse/count/health");

        return countService.getWarehouseHealthMetrics()
                .map(response -> {
                    EntityModel<WarehouseCountDTO.HealthMetricsResponse> resource = EntityModel.of(response);
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getHealthMetrics()).withSelfRel());
                    resource.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseCountController.class)
                            .getStatistics()).withRel("statistics"));
                    return ResponseEntity.ok(resource);
                });
    }
}
