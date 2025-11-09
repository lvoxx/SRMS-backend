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
         * Create a new warehouse
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
         * Find warehouse by ID
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
         * Find warehouse by product name
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
         * Find all warehouses with pagination
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
         * Find all warehouses with filters
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
         * Update warehouse (without quantity)
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
         * Process inventory transaction (import/export)
         * This creates a history record and the database trigger will update the
         * quantity
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
         * Import inventory
         * README: Already evict cache in processInventoryTransaction, no need to evict
         * here
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
         * Export inventory
         * README: Already evict cache in processInventoryTransaction, no need to evict
         * here
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
         * Soft delete warehouse
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
         * Restore deleted warehouse
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
         * Permanently delete warehouse (hard delete)
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
         * Batch create warehouses
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
         * Batch soft delete warehouses
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
         * Validate product name uniqueness
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
         * Validate inventory transaction
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