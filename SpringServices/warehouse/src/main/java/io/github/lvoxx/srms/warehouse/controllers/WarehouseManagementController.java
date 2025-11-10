package io.github.lvoxx.srms.warehouse.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.services.WarehouseManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for warehouse management operations.
 * <p>
 * Provides comprehensive CRUD operations, inventory transactions
 * (import/export),
 * soft delete/restore functionality, and batch operations.
 * All modification operations use user ID from JWT claims passed via X-User-Id
 * header
 * from the Kubernetes nginx gateway.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/management")
@RequiredArgsConstructor
public class WarehouseManagementController {

    private final WarehouseManagementService managementService;

    // ==================== CREATE ====================

    /**
     * Creates a new warehouse record.
     * <p>
     * Validates product name uniqueness and creates warehouse with initial
     * quantity.
     * If initial quantity > 0, an import transaction is automatically created.
     * 
     * @param request warehouse creation request with validation
     * @param userId  user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with created warehouse (HTTP 201)
     */
    @PostMapping
    public Mono<ResponseEntity<WarehouseDTO.Response>> createWarehouse(
            @Valid @RequestBody WarehouseDTO.Request request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("POST /warehouse/management - Creating: {}", request.getProductName());

        return managementService.createWarehouse(request, userId)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Batch creates multiple warehouses.
     * <p>
     * Processes multiple warehouse creation requests. Individual errors are logged
     * but don't stop the batch process. Returns count of successfully created
     * warehouses.
     * 
     * @param requests Flux of warehouse creation requests with validation
     * @param userId   user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with count of created warehouses
     */
    @PostMapping("/batch")
    public Mono<ResponseEntity<Long>> batchCreate(
            @Valid @RequestBody Flux<WarehouseDTO.Request> requests,
            @RequestHeader("X-User-Id") String userId) {
        log.info("POST /warehouse/management/batch");

        return managementService.batchCreate(requests, userId)
                .count()
                .map(ResponseEntity::ok);
    }

    // ==================== READ ====================

    /**
     * Retrieves warehouse by unique identifier.
     * 
     * @param id             warehouse unique identifier
     * @param includeDeleted whether to include soft-deleted warehouses
     * @return Mono emitting ResponseEntity with warehouse and HATEOAS links
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<EntityModel<WarehouseDTO.Response>>> findById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.debug("GET /warehouse/management/{}", id);

        return managementService.findById(id, includeDeleted)
                .map(response -> {
                    EntityModel<WarehouseDTO.Response> resource = EntityModel.of(response);
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findById(id, includeDeleted)).withSelfRel());
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findAll(includeDeleted, 0, 20)).withRel("collection"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Retrieves warehouse by product name.
     * 
     * @param productName    product name to search for
     * @param includeDeleted whether to include soft-deleted warehouses
     * @return Mono emitting ResponseEntity with warehouse and HATEOAS links
     */
    @GetMapping("/by-name/{productName}")
    public Mono<ResponseEntity<EntityModel<WarehouseDTO.Response>>> findByProductName(
            @PathVariable String productName,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.debug("GET /warehouse/management/by-name/{}", productName);

        return managementService.findByProductName(productName, includeDeleted)
                .map(response -> {
                    EntityModel<WarehouseDTO.Response> resource = EntityModel.of(response);
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findByProductName(productName, includeDeleted)).withSelfRel());
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findById(response.getId(), includeDeleted)).withRel("details"));
                    return ResponseEntity.ok(resource);
                });
    }

    /**
     * Retrieves all warehouses with pagination.
     * 
     * @param includeDeleted whether to include soft-deleted warehouses
     * @param page           zero-based page index (default: 0)
     * @param size           number of items per page (default: 20)
     * @return Mono emitting ResponseEntity with collection of warehouses and
     *         HATEOAS links
     */
    @GetMapping
    public Mono<ResponseEntity<CollectionModel<EntityModel<WarehouseDTO.Response>>>> findAll(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        log.debug("GET /warehouse/management - page: {}, size: {}", page, size);

        return managementService.findAll(includeDeleted, page, size)
                .map(response -> {
                    EntityModel<WarehouseDTO.Response> resource = EntityModel.of(response);
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findById(response.getId(), includeDeleted)).withSelfRel());
                    return resource;
                })
                .collectList()
                .map(resources -> {
                    CollectionModel<EntityModel<WarehouseDTO.Response>> collection = CollectionModel.of(resources);
                    collection.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findAll(includeDeleted, page, size)).withSelfRel());
                    if (page > 0) {
                        collection.add(
                                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                        .findAll(includeDeleted, page - 1, size)).withRel("previous"));
                    }
                    collection.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findAll(includeDeleted, page + 1, size)).withRel("next"));
                    return ResponseEntity.ok(collection);
                });
    }

    /**
     * Retrieves warehouses with complex filtering and pagination.
     * <p>
     * Supports multiple optional filters that can be combined:
     * product name (partial match), quantity range, creation date range, and update
     * date range.
     * 
     * @param includeDeleted whether to include soft-deleted warehouses
     * @param productName    product name filter (partial match, optional)
     * @param minQuantity    minimum quantity threshold (optional)
     * @param maxQuantity    maximum quantity threshold (optional)
     * @param createdFrom    start of creation date range (optional)
     * @param createdTo      end of creation date range (optional)
     * @param updatedFrom    start of update date range (optional)
     * @param updatedTo      end of update date range (optional)
     * @param page           zero-based page index (default: 0)
     * @param size           number of items per page (default: 20)
     * @return Mono emitting ResponseEntity with filtered collection and HATEOAS
     *         links
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<CollectionModel<EntityModel<WarehouseDTO.Response>>>> findAllWithFilters(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @Min(0) Integer minQuantity,
            @RequestParam(required = false) @Min(0) Integer maxQuantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime updatedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime updatedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        log.debug("GET /warehouse/management/search");

        return managementService.findAllWithFilters(includeDeleted, productName, minQuantity, maxQuantity,
                createdFrom, createdTo, updatedFrom, updatedTo, page, size)
                .map(response -> {
                    EntityModel<WarehouseDTO.Response> resource = EntityModel.of(response);
                    resource.add(
                            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                    .findById(response.getId(), includeDeleted)).withSelfRel());
                    return resource;
                })
                .collectList()
                .map(resources -> {
                    CollectionModel<EntityModel<WarehouseDTO.Response>> collection = CollectionModel.of(resources);
                    collection
                            .add(WebMvcLinkBuilder
                                    .linkTo(WebMvcLinkBuilder.methodOn(WarehouseManagementController.class)
                                            .findAllWithFilters(includeDeleted, productName, minQuantity, maxQuantity,
                                                    createdFrom, createdTo, updatedFrom, updatedTo, page, size))
                                    .withSelfRel());
                    return ResponseEntity.ok(collection);
                });
    }

    // ==================== UPDATE ====================

    /**
     * Updates warehouse information (excluding quantity).
     * <p>
     * Updates product name, minimum quantity threshold, and contactor ID.
     * Quantity updates must be done through inventory transaction endpoints.
     * Uses distributed row-level locking to prevent concurrent modifications.
     * 
     * @param id      warehouse unique identifier
     * @param request update request with validation
     * @param userId  user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with updated warehouse
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<WarehouseDTO.Response>> updateWarehouse(
            @PathVariable UUID id,
            @Valid @RequestBody WarehouseDTO.UpdateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        log.info("PUT /warehouse/management/{}", id);

        return managementService.updateWarehouse(id, request, userId)
                .map(ResponseEntity::ok);
    }

    // ==================== INVENTORY TRANSACTIONS ====================

    /**
     * Processes inventory transaction (import or export).
     * <p>
     * Acquires distributed lock, validates transaction, creates history record,
     * and updates warehouse quantity automatically via database trigger.
     * 
     * @param request inventory transaction request with validation
     * @return Mono emitting ResponseEntity with updated warehouse
     */
    @PostMapping("/transaction")
    public Mono<ResponseEntity<WarehouseDTO.Response>> processTransaction(
            @Valid @RequestBody WarehouseDTO.InventoryTransactionRequest request) {
        log.info("POST /warehouse/management/transaction - {} for warehouse {}",
                request.getType(), request.getWarehouseId());

        return managementService.processInventoryTransaction(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Imports inventory to warehouse.
     * <p>
     * Convenience endpoint for import operations.
     * Quantity must be positive and will be added to current warehouse quantity.
     * 
     * @param warehouseId warehouse unique identifier
     * @param quantity    quantity to import (must be >= 1)
     * @param userId      user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with updated warehouse
     */
    @PatchMapping("/{warehouseId}/import")
    public Mono<ResponseEntity<WarehouseDTO.Response>> importInventory(
            @PathVariable UUID warehouseId,
            @RequestParam @Min(1) int quantity,
            @RequestHeader("X-User-Id") String userId) {
        log.info("PATCH /warehouse/management/{}/import - quantity: {}", warehouseId, quantity);

        return managementService.importInventory(warehouseId, quantity, userId)
                .map(ResponseEntity::ok);
    }

    /**
     * Exports inventory from warehouse.
     * <p>
     * Convenience endpoint for export operations.
     * Validates sufficient inventory exists before proceeding.
     * Quantity must be positive and cannot exceed current warehouse quantity.
     * 
     * @param warehouseId warehouse unique identifier
     * @param quantity    quantity to export (must be >= 1)
     * @param userId      user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with updated warehouse
     */
    @PatchMapping("/{warehouseId}/export")
    public Mono<ResponseEntity<WarehouseDTO.Response>> exportInventory(
            @PathVariable UUID warehouseId,
            @RequestParam @Min(1) int quantity,
            @RequestHeader("X-User-Id") String userId) {
        log.info("PATCH /warehouse/management/{}/export - quantity: {}", warehouseId, quantity);

        return managementService.exportInventory(warehouseId, quantity, userId)
                .map(ResponseEntity::ok);
    }

    // ==================== DELETE ====================

    /**
     * Performs soft delete on warehouse.
     * <p>
     * Marks warehouse as deleted without removing from database.
     * Can be restored later using restore endpoint.
     * Preserves historical data and maintains referential integrity.
     * 
     * @param id     warehouse unique identifier
     * @param userId user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with no content (HTTP 204)
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> softDelete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("DELETE /warehouse/management/{}", id);

        return managementService.softDelete(id, userId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    /**
     * Restores previously soft-deleted warehouse.
     * <p>
     * Reverses soft delete by clearing deletion flags and timestamps.
     * Can only restore warehouses currently marked as deleted.
     * 
     * @param id     warehouse unique identifier
     * @param userId user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with restored warehouse
     */
    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<WarehouseDTO.Response>> restore(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("PATCH /warehouse/management/{}/restore", id);

        return managementService.restore(id, userId)
                .map(ResponseEntity::ok);
    }

    /**
     * Permanently deletes warehouse (hard delete).
     * <p>
     * WARNING: This permanently removes the warehouse from database.
     * Cannot be undone. Use with extreme caution.
     * 
     * @param id warehouse unique identifier
     * @return Mono emitting ResponseEntity with no content (HTTP 204)
     */
    @DeleteMapping("/{id}/permanent")
    public Mono<ResponseEntity<Void>> permanentDelete(@PathVariable UUID id) {
        log.warn("DELETE /warehouse/management/{}/permanent", id);

        return managementService.permanentDelete(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    /**
     * Batch soft deletes warehouses.
     * <p>
     * Processes multiple warehouse deletions.
     * Individual errors are logged but don't stop the batch process.
     * Returns count of successfully deleted warehouses.
     * 
     * @param ids    Flux of warehouse IDs to delete
     * @param userId user ID from JWT claims (X-User-Id header)
     * @return Mono emitting ResponseEntity with count of deleted warehouses
     */
    @DeleteMapping("/batch")
    public Mono<ResponseEntity<Long>> batchSoftDelete(
            @RequestBody Flux<UUID> ids,
            @RequestHeader("X-User-Id") String userId) {
        log.info("DELETE /warehouse/management/batch");

        return managementService.batchSoftDelete(ids, userId)
                .map(ResponseEntity::ok);
    }
}