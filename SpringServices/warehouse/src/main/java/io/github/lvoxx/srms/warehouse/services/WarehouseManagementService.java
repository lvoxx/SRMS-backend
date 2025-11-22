package io.github.lvoxx.srms.warehouse.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.srms.common.cache.TableLockerNames;
import io.github.lvoxx.srms.common.cache.WarehouseCacheNames;
import io.github.lvoxx.srms.controllerhandler.model.ConflictException;
import io.github.lvoxx.srms.controllerhandler.model.DataPersistantException;
import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.controllerhandler.model.ValidationException;
import io.github.lvoxx.srms.redisson.services.ReactiveRowLockService;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.mapper.WarehouseMapper;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Core service for warehouse management operations.
 * <p>
 * Handles CRUD operations, inventory transactions (import/export), soft
 * deletes,
 * restores, batch operations, and data validation.
 * <p>
 * Uses distributed row-level locking via Redisson for concurrency control.
 * Implements comprehensive cache eviction policies to maintain data freshness.
 * All data-modifying operations are transactional.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseManagementService {

        private final WarehouseRepository warehouseRepository;
        private final WarehouseHistoryRepository warehouseHistoryRepository;
        private final ReactiveRowLockService lockService;
        private final WarehouseMapper warehouseMapper;

        // ==================== CREATE ====================

        /**
         * Creates a new warehouse record with optional initial stock.
         * <p>
         * Validates product name uniqueness, creates warehouse with quantity = 0,
         * and if initial quantity provided, creates import history record.
         * 
         * @param request   warehouse creation request containing product details
         * @param createdBy username/ID of user creating the warehouse
         * @return Mono emitting created warehouse response
         * @throws ConflictException       if product name already exists
         * @throws DataPersistantException if database save fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_STATISTICS, allEntries = true)
        })
        public Mono<WarehouseDTO.Response> createWarehouse(
                        WarehouseDTO.Request request, String createdBy) {
                log.info("Creating warehouse: {}", request.getProductName());

                return validateProductNameUnique(request.getProductName(), null)
                                .then(Mono.defer(() -> {
                                        Warehouse warehouse = warehouseMapper.toEntity(request);
                                        warehouse.setCreatedAt(OffsetDateTime.now());
                                        warehouse.setUpdatedAt(OffsetDateTime.now());
                                        warehouse.setLastUpdatedBy(createdBy.toString());
                                        warehouse.setQuantity(0);
                                        return warehouseRepository.save(warehouse)
                                                        .flatMap(savedWarehouse -> {
                                                                // Create initial stock as import transaction if
                                                                // quantity > 0
                                                                if (request.getQuantity() != null
                                                                                && request.getQuantity() > 0) {
                                                                        WarehouseHistory history = WarehouseHistory
                                                                                        .builder()
                                                                                        .warehouseId(savedWarehouse
                                                                                                        .getId())
                                                                                        .quantity(request.getQuantity())
                                                                                        .type(WarehouseHistory.HistoryType.IMPORT
                                                                                                        .getValue())
                                                                                        .updatedBy(createdBy.toString())
                                                                                        .createdAt(OffsetDateTime.now())
                                                                                        .build();

                                                                        return warehouseHistoryRepository.save(history)
                                                                                        .then(warehouseRepository
                                                                                                        .findById(savedWarehouse
                                                                                                                        .getId(),
                                                                                                                        false));
                                                                }
                                                                return Mono.just(savedWarehouse);
                                                        })
                                                        .doOnSuccess(w -> log.info("Warehouse created successfully: {}",
                                                                        w.getId()))
                                                        .map(warehouseMapper::toResponse)
                                                        .onErrorResume(e -> {
                                                                log.error("Error creating warehouse: {}",
                                                                                e.getMessage(), e);
                                                                return Mono.error(new DataPersistantException(
                                                                                "Failed to create warehouse: "
                                                                                                + e.getMessage()));
                                                        });
                                }));
        }

        // ==================== READ ====================

        /**
         * Retrieves warehouse by unique identifier.
         * 
         * @param id             unique identifier of the warehouse
         * @param includeDeleted true to include soft-deleted warehouses
         * @return Mono emitting warehouse response
         * @throws NotFoundException       if warehouse not found
         * @throws InternalServerException if database operation fails
         */
        @Cacheable(value = WarehouseCacheNames.DETAILS, key = "#id + ':' + #includeDeleted")
        public Mono<WarehouseDTO.Response> findById(UUID id, boolean includeDeleted) {
                log.debug("Finding warehouse by ID: {}", id);

                return warehouseRepository.findById(id, includeDeleted)
                                .map(warehouseMapper::toResponse)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                "Warehouse not found with id: " + id)))
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException)
                                                return Mono.error(e);
                                        log.error("Error finding warehouse by ID {}: {}", id, e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to find warehouse: " + e.getMessage()));
                                });
        }

        /**
         * Retrieves warehouse by product name.
         * 
         * @param productName    name of the product to search for
         * @param includeDeleted true to include soft-deleted warehouses
         * @return Mono emitting warehouse response
         * @throws NotFoundException       if warehouse not found
         * @throws InternalServerException if database operation fails
         */
        @Cacheable(value = WarehouseCacheNames.BY_NAME, key = "#productName + ':' + #includeDeleted")
        public Mono<WarehouseDTO.Response> findByProductName(
                        String productName, boolean includeDeleted) {
                log.debug("Finding warehouse by product name: {}", productName);

                return warehouseRepository.findByProductName(productName, includeDeleted)
                                .map(warehouseMapper::toResponse)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                "Warehouse not found with product name: " + productName)))
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException)
                                                return Mono.error(e);
                                        log.error("Error finding warehouse by name {}: {}",
                                                        productName, e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to find warehouse: " + e.getMessage()));
                                });
        }

        /**
         * Retrieves all warehouses with pagination support.
         * 
         * @param includeDeleted true to include soft-deleted warehouses
         * @param page           zero-based page index
         * @param size           number of items per page
         * @return Flux emitting warehouse responses for requested page
         * @throws InternalServerException if database operation fails
         */
        public Flux<WarehouseDTO.Response> findAll(
                        boolean includeDeleted, int page, int size) {
                log.debug("Finding all warehouses - page: {}, size: {}", page, size);

                Pageable pageable = PageRequest.of(page, size);

                return warehouseRepository.findAll(includeDeleted, pageable)
                                .map(warehouseMapper::toResponse)
                                .onErrorResume(e -> {
                                        log.error("Error finding all warehouses: {}", e.getMessage(), e);
                                        return Flux.error(new InternalServerException(
                                                        "Failed to retrieve warehouses: " + e.getMessage()));
                                });
        }

        /**
         * Retrieves warehouses with complex filtering and pagination.
         * <p>
         * Supports filtering by product name, quantity range, creation date range, and
         * update date range.
         * All filter parameters are optional and can be combined.
         * 
         * @param includeDeleted true to include soft-deleted warehouses
         * @param productName    partial product name to search (nullable)
         * @param minQuantity    minimum quantity threshold (nullable)
         * @param maxQuantity    maximum quantity threshold (nullable)
         * @param createdFrom    start of creation date range (nullable)
         * @param createdTo      end of creation date range (nullable)
         * @param updatedFrom    start of update date range (nullable)
         * @param updatedTo      end of update date range (nullable)
         * @param page           zero-based page index
         * @param size           number of items per page
         * @return Flux emitting filtered warehouse responses
         * @throws InternalServerException if database operation fails
         */
        public Flux<WarehouseDTO.Response> findAllWithFilters(
                        boolean includeDeleted,
                        String productName,
                        Integer minQuantity,
                        Integer maxQuantity,
                        OffsetDateTime createdFrom,
                        OffsetDateTime createdTo,
                        OffsetDateTime updatedFrom,
                        OffsetDateTime updatedTo,
                        int page,
                        int size) {
                log.debug("Finding warehouses with filters");

                Pageable pageable = PageRequest.of(page, size);

                return warehouseRepository.findAllWithFilters(
                                includeDeleted, productName, minQuantity, maxQuantity,
                                createdFrom, createdTo, updatedFrom, updatedTo, pageable)
                                .map(warehouseMapper::toResponse)
                                .onErrorResume(e -> {
                                        log.error("Error finding warehouses with filters: {}", e.getMessage(), e);
                                        return Flux.error(new InternalServerException(
                                                        "Failed to retrieve warehouses: " + e.getMessage()));
                                });
        }

        // ==================== UPDATE ====================

        /**
         * Updates warehouse information (excluding quantity).
         * <p>
         * Updates product name, minimum quantity threshold, and contactor ID.
         * Quantity updates must be done through inventory transactions.
         * Uses distributed row-level locking to prevent concurrent modifications.
         * 
         * @param id        unique identifier of warehouse to update
         * @param request   update request containing new values
         * @param updatedBy username/ID of user performing update
         * @return Mono emitting updated warehouse response
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws ConflictException       if new product name conflicts
         * @throws DataPersistantException if update operation fails
         * @throws InternalServerException if lock acquisition or database operation
         *                                 fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, key = "#id + ':false'"),
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, key = "#id + ':true'"),
                        @CacheEvict(value = WarehouseCacheNames.BY_NAME, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DETAILS, key = "#id"),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true)
        })
        public Mono<WarehouseDTO.Response> updateWarehouse(
                        UUID id,
                        WarehouseDTO.UpdateRequest request,
                        String updatedBy) {

                log.info("Updating warehouse: {}", id);

                return lockService.acquireLock(TableLockerNames.WAREHOUSE, id)
                                .flatMap(lock -> warehouseRepository.findById(id, false)
                                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                                "Warehouse not found with id: " + id)))
                                                .flatMap(existing -> validateProductNameUnique(request.getProductName(),
                                                                id)
                                                                .thenReturn(existing))
                                                .flatMap(existing -> {
                                                        OffsetDateTime now = OffsetDateTime.now();

                                                        return warehouseRepository.updateWarehouse(
                                                                        id,
                                                                        request.getProductName(),
                                                                        request.getMinQuantity(),
                                                                        request.getContactorId(),
                                                                        now,
                                                                        updatedBy)
                                                                        .flatMap(updated -> {
                                                                                if (updated == 0) {
                                                                                        return Mono.error(
                                                                                                        new DataPersistantException(
                                                                                                                        "Warehouse not found or already deleted"));
                                                                                }
                                                                                return warehouseRepository.findById(id,
                                                                                                false);
                                                                        })
                                                                        .map(warehouseMapper::toResponse)
                                                                        .doOnSuccess(w -> log.info(
                                                                                        "Warehouse updated successfully: {}",
                                                                                        id));
                                                })
                                                .doFinally(signal -> lockService.releaseLock(lock).subscribe()) // !
                                                                                                                // IMPORTANT:
                                                                                                                // Remember
                                                                                                                // to
                                                                                                                // release
                                                                                                                // lock
                                )
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException || e instanceof ConflictException) {
                                                return Mono.error(e);
                                        }
                                        log.error("Error updating warehouse {}: {}", id, e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to update warehouse: " + e.getMessage()));
                                });
        }

        // ==================== INVENTORY TRANSACTIONS ====================

        /**
         * Processes inventory transaction (import or export).
         * <p>
         * Acquires distributed lock, validates transaction, creates history record,
         * and relies on database trigger to update warehouse quantity automatically.
         * Validates sufficient stock for exports and positive quantities.
         * 
         * @param request transaction request containing warehouse ID, quantity, and
         *                type
         * @return Mono emitting updated warehouse response
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws ValidationException     if validation fails (insufficient stock,
         *                                 invalid quantity)
         * @throws InternalServerException if lock acquisition or database operation
         *                                 fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DETAILS, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_TOTAL_IMPORT, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_TOTAL_EXPORT, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_BALANCE, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_BELOW_MINIMUM, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_OUT_OF_STOCK, allEntries = true)
        })
        public Mono<WarehouseDTO.Response> processInventoryTransaction(
                        WarehouseDTO.InventoryTransactionRequest request) {

                log.info("Processing {} transaction for warehouse: {}",
                                request.getType(), request.getWarehouseId());

                UUID warehouseId = request.getWarehouseId();

                return lockService.acquireLock(TableLockerNames.WAREHOUSE_HISTORY, warehouseId)
                                .flatMap(lock -> warehouseRepository.findById(warehouseId, false)
                                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                                "Warehouse not found with id: " + warehouseId)))
                                                .flatMap(warehouse -> {
                                                        validateInventoryTransaction(warehouse, request);

                                                        // Create history record - trigger will update warehouse
                                                        // quantity
                                                        WarehouseHistory history = warehouseMapper
                                                                        .toHistoryEntity(request);
                                                        history.setCreatedAt(OffsetDateTime.now());
                                                        history.setType(request.getType().getValue());

                                                        return warehouseHistoryRepository.save(history)
                                                                        .doOnSuccess(h -> log.debug(
                                                                                        "History record created: {}",
                                                                                        h.getId()))
                                                                        .then(warehouseRepository.findById(
                                                                                        request.getWarehouseId(),
                                                                                        false))
                                                                        .map(warehouseMapper::toResponse)
                                                                        .doOnSuccess(w -> log.info(
                                                                                        "Transaction processed successfully: {} {} for warehouse {}",
                                                                                        request.getType(),
                                                                                        request.getQuantity(),
                                                                                        request.getWarehouseId()));
                                                })
                                                .doFinally(signal -> lockService.releaseLock(lock).subscribe()))
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException || e instanceof ValidationException) {
                                                return Mono.error(e);
                                        }
                                        log.error("Error processing transaction for warehouse {}: {}", warehouseId,
                                                        e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to process transaction: " + e.getMessage()));
                                });
        }

        /**
         * Convenience method to import inventory to warehouse.
         * 
         * @param warehouseId unique identifier of warehouse
         * @param quantity    quantity to import (must be positive)
         * @param updatedBy   username/ID of user performing import
         * @return Mono emitting updated warehouse response
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws ValidationException     if quantity is invalid
         * @throws InternalServerException if operation fails
         */
        @Transactional
        public Mono<WarehouseDTO.Response> importInventory(
                        UUID warehouseId, int quantity, String updatedBy) {
                log.info("Importing {} units to warehouse: {}", quantity, warehouseId);

                WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest.builder()
                                .warehouseId(warehouseId)
                                .quantity(quantity)
                                .type(WarehouseHistory.HistoryType.IMPORT)
                                .updatedBy(updatedBy)
                                .build();

                return processInventoryTransaction(request); // Already have lock
        }

        /**
         * Convenience method to export inventory from warehouse.
         * <p>
         * Validates sufficient inventory exists before proceeding.
         * 
         * @param warehouseId unique identifier of warehouse
         * @param quantity    quantity to export (must be positive and <= current
         *                    quantity)
         * @param updatedBy   username/ID of user performing export
         * @return Mono emitting updated warehouse response
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws ValidationException     if quantity invalid or exceeds available
         *                                 stock
         * @throws InternalServerException if operation fails
         */
        @Transactional
        public Mono<WarehouseDTO.Response> exportInventory(
                        UUID warehouseId, int quantity, String updatedBy) {
                log.info("Exporting {} units from warehouse: {}", quantity, warehouseId);

                WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest.builder()
                                .warehouseId(warehouseId)
                                .quantity(quantity)
                                .type(WarehouseHistory.HistoryType.EXPORT)
                                .updatedBy(updatedBy)
                                .build();

                return processInventoryTransaction(request); // Already have lock
        }

        // ==================== DELETE ====================

        /**
         * Performs soft delete on warehouse.
         * <p>
         * Marks warehouse as deleted without removing from database.
         * Can be restored later. Preserves historical data and maintains referential
         * integrity.
         * 
         * @param id        unique identifier of warehouse to delete
         * @param deletedBy username/ID of user performing deletion
         * @return Mono that completes when deletion is successful
         * @throws NotFoundException       if warehouse doesn't exist or already deleted
         * @throws DataPersistantException if database update fails
         * @throws InternalServerException if operation fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, key = "#id + ':false'"),
                        @CacheEvict(value = WarehouseCacheNames.BY_NAME, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_STATISTICS, allEntries = true)
        })
        public Mono<Void> softDelete(UUID id, String deletedBy) {
                log.info("Soft deleting warehouse: {}", id);

                return warehouseRepository.findById(id, false)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                "Warehouse not found with id: " + id)))
                                .flatMap(warehouse -> {
                                        OffsetDateTime now = OffsetDateTime.now();

                                        return warehouseRepository.softDelete(id, now, deletedBy)
                                                        .flatMap(deleted -> {
                                                                if (deleted == 0) {
                                                                        return Mono.<Void>error(
                                                                                        new DataPersistantException(
                                                                                                        "Warehouse not found or already deleted"));
                                                                }
                                                                return Mono.<Void>empty();
                                                        })
                                                        .doOnSuccess(v -> log.info(
                                                                        "Warehouse soft deleted successfully: {}", id));
                                })
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException)
                                                return Mono.<Void>error(e);
                                        log.error("Error soft deleting warehouse {}: {}", id, e.getMessage(), e);
                                        return Mono.<Void>error(new InternalServerException(
                                                        "Failed to delete warehouse: " + e.getMessage()));
                                });
        }

        /**
         * Restores previously soft-deleted warehouse.
         * <p>
         * Reverses soft delete by clearing deletion flags and timestamps.
         * Can only restore warehouses currently marked as deleted.
         * 
         * @param id         unique identifier of warehouse to restore
         * @param restoredBy username/ID of user performing restoration
         * @return Mono emitting restored warehouse response
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws ConflictException       if warehouse is not currently deleted
         * @throws DataPersistantException if database update fails
         * @throws InternalServerException if operation fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, key = "#id + ':true'"),
                        @CacheEvict(value = WarehouseCacheNames.BY_NAME, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_STATISTICS, allEntries = true)
        })
        public Mono<WarehouseDTO.Response> restore(UUID id, String restoredBy) {
                log.info("Restoring warehouse: {}", id);

                return warehouseRepository.findById(id, true)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                "Warehouse not found with id: " + id)))
                                .flatMap(warehouse -> {
                                        if (!warehouse.getIsDeleted()) {
                                                return Mono.error(new ConflictException(
                                                                "Warehouse is not deleted"));
                                        }

                                        OffsetDateTime now = OffsetDateTime.now();

                                        return warehouseRepository.restore(id, now, restoredBy)
                                                        .flatMap(restored -> {
                                                                if (restored == 0) {
                                                                        return Mono.error(new DataPersistantException(
                                                                                        "Failed to restore warehouse"));
                                                                }
                                                                return warehouseRepository.findById(id, false);
                                                        })
                                                        .map(warehouseMapper::toResponse)
                                                        .doOnSuccess(w -> log.info(
                                                                        "Warehouse restored successfully: {}", id));
                                })
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException)
                                                return Mono.error(e);
                                        if (e instanceof ConflictException)
                                                return Mono.error(e);
                                        log.error("Error restoring warehouse {}: {}", id, e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to restore warehouse: " + e.getMessage()));
                                });
        }

        /**
         * Permanently deletes warehouse (hard delete).
         * <p>
         * WARNING: This permanently removes the warehouse from database.
         * Cannot be undone. Use with caution.
         * 
         * @param id unique identifier of warehouse to permanently delete
         * @return Mono that completes when deletion is successful
         * @throws NotFoundException       if warehouse doesn't exist
         * @throws InternalServerException if operation fails
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.BY_NAME, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_STATISTICS, allEntries = true)
        })
        public Mono<Void> permanentDelete(UUID id) {
                log.warn("Permanently deleting warehouse: {}", id);

                return warehouseRepository.findById(id, true)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                "Warehouse not found with id: " + id)))
                                .flatMap(warehouse -> warehouseRepository.deleteById(id)
                                                .doOnSuccess(v -> log.info("Warehouse permanently deleted: {}", id)))
                                .onErrorResume(e -> {
                                        if (e instanceof NotFoundException)
                                                return Mono.error(e);
                                        log.error("Error permanently deleting warehouse {}: {}",
                                                        id, e.getMessage(), e);
                                        return Mono.error(new InternalServerException(
                                                        "Failed to permanently delete warehouse: " + e.getMessage()));
                                });
        }

        // ==================== BATCH OPERATIONS ====================

        /**
         * Batch creates warehouses.
         * <p>
         * Processes multiple warehouse creation requests.
         * Errors for individual items are logged but don't stop the batch.
         * 
         * @param requests  Flux of warehouse creation requests
         * @param createdBy username/ID of user creating warehouses
         * @return Flux emitting successfully created warehouse responses
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true)
        })
        public Flux<WarehouseDTO.Response> batchCreate(
                        Flux<WarehouseDTO.Request> requests, String createdBy) {
                log.info("Batch creating warehouses");

                return requests
                                .flatMap(request -> createWarehouse(request, createdBy)
                                                .onErrorResume(e -> {
                                                        log.error("Error creating warehouse {}: {}",
                                                                        request.getProductName(), e.getMessage());
                                                        return Mono.empty();
                                                }))
                                .doOnComplete(() -> log.info("Batch create completed"));
        }

        /**
         * Batch soft deletes warehouses.
         * <p>
         * Processes multiple warehouse deletions.
         * Errors for individual items are logged but don't stop the batch.
         * 
         * @param ids       Flux of warehouse IDs to delete
         * @param deletedBy username/ID of user performing deletions
         * @return Mono emitting count of successfully deleted warehouses
         */
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = WarehouseCacheNames.DETAILS, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.COUNT_ALL, allEntries = true),
                        @CacheEvict(value = WarehouseCacheNames.STATS_DASHBOARD, allEntries = true)
        })
        public Mono<Long> batchSoftDelete(Flux<UUID> ids, String deletedBy) {
                log.info("Batch soft deleting warehouses");

                return ids
                                .flatMap(id -> softDelete(id, deletedBy)
                                                .thenReturn(1L)
                                                .onErrorResume(e -> {
                                                        log.error("Error deleting warehouse {}: {}", id,
                                                                        e.getMessage());
                                                        return Mono.just(0L);
                                                }))
                                .reduce(0L, Long::sum)
                                .doOnSuccess(count -> log.info("Batch delete completed: {} deleted", count));
        }

        // ==================== HELPER METHODS ====================

        /**
         * Validates product name uniqueness.
         * <p>
         * Ensures product name doesn't conflict with existing warehouses.
         * Can exclude a specific warehouse ID for update operations.
         * 
         * @param productName product name to validate
         * @param excludeId   warehouse ID to exclude from uniqueness check (nullable)
         * @return Mono that completes if valid, errors if duplicate found
         * @throws ConflictException if product name already exists
         */
        private Mono<Void> validateProductNameUnique(String productName, UUID excludeId) {
                if (productName == null || productName.isBlank()) {
                        return Mono.empty();
                }

                return warehouseRepository.findByProductName(productName, false)
                                .flatMap(existing -> {
                                        if (excludeId == null || !existing.getId().equals(excludeId)) {
                                                return Mono.<Void>error(new ConflictException(
                                                                "Product name already exists: " + productName));
                                        }
                                        return Mono.empty();
                                })
                                .then();
        }

        /**
         * Validates inventory transaction.
         * <p>
         * For exports: validates sufficient inventory exists.
         * For all transactions: validates quantity is positive.
         * 
         * @param warehouse warehouse entity to validate against
         * @param request   transaction request to validate
         * @throws ValidationException if validation fails
         */
        private void validateInventoryTransaction(
                        Warehouse warehouse,
                        WarehouseDTO.InventoryTransactionRequest request) {

                if (request.getType() == WarehouseHistory.HistoryType.EXPORT) {
                        if (warehouse.getQuantity() < request.getQuantity()) {
                                throw new ValidationException(
                                                String.format("Insufficient inventory. Available: %d, Requested: %d",
                                                                warehouse.getQuantity(), request.getQuantity()));
                        }
                }

                if (request.getQuantity() <= 0) {
                        throw new ValidationException("Quantity must be greater than 0");
                }
        }

}