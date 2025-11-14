package io.github.lvoxx.srms.warehouse.controllers;

import java.util.UUID;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.reactive.WebFluxLinkBuilder;
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

import io.github.lvoxx.srms.controllerhandler.model.ValidationException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.dto.WarehouseSearchDTO;
import io.github.lvoxx.srms.warehouse.services.WarehouseManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for warehouse management operations.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/management")
@RequiredArgsConstructor
public class WarehouseManagementController {

        private final WarehouseManagementService managementService;

        // ==================== CREATE ====================

        @PostMapping
        public Mono<ResponseEntity<WarehouseDTO.Response>> createWarehouse(
                        @Valid @RequestBody WarehouseDTO.Request request,
                        @RequestHeader("X-User-Id") String userId) {
                log.info("POST /warehouse/management - Creating: {}", request.getProductName());

                return managementService.createWarehouse(request, userId)
                                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
        }

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

        @GetMapping("/{id}")
        public Mono<ResponseEntity<EntityModel<WarehouseDTO.Response>>> findById(
                        @PathVariable UUID id,
                        @RequestParam(defaultValue = "false") boolean includeDeleted) {
                log.debug("GET /warehouse/management/{}", id);

                return managementService.findById(id, includeDeleted)
                                .flatMap(response -> toEntityResourceWithCollection(response, includeDeleted))
                                .map(ResponseEntity::ok);
        }

        @GetMapping("/by-name/{productName}")
        public Mono<ResponseEntity<EntityModel<WarehouseDTO.Response>>> findByProductName(
                        @PathVariable String productName,
                        @RequestParam(defaultValue = "false") boolean includeDeleted) {
                log.debug("GET /warehouse/management/by-name/{}", productName);

                return managementService.findByProductName(productName, includeDeleted)
                                .flatMap(response -> toEntityResourceWithDetails(response, productName, includeDeleted))
                                .map(ResponseEntity::ok);
        }

        @GetMapping
        public Mono<ResponseEntity<CollectionModel<EntityModel<WarehouseDTO.Response>>>> findAll(
                        @RequestParam(defaultValue = "false") boolean includeDeleted,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "20") @Min(1) int size) {
                log.debug("GET /warehouse/management - page: {}, size: {}", page, size);

                return managementService.findAll(includeDeleted, page, size)
                                .flatMap(response -> toEntityResource(response, includeDeleted))
                                .collectList()
                                .flatMap(resources -> toSimpleCollectionResource(Flux.fromIterable(resources),
                                                includeDeleted, page, size))
                                .map(ResponseEntity::ok);
        }

        @PostMapping("/search")
        public Mono<ResponseEntity<CollectionModel<EntityModel<WarehouseDTO.Response>>>> findAllWithFilters(
                        @RequestParam(defaultValue = "false") boolean includeDeleted,
                        @Valid @RequestBody WarehouseSearchDTO.Request searchRequest) {
                log.debug("GET /warehouse/management/search");

                // Manual validation for additional business rules
                try {
                        searchRequest.validate();
                } catch (ValidationException e) {
                        return Mono.error(e);
                }

                return managementService.findAllWithFilters(
                                includeDeleted,
                                searchRequest.getProductName(),
                                searchRequest.getMinQuantity(),
                                searchRequest.getMaxQuantity(),
                                searchRequest.getParsedCreatedFrom(),
                                searchRequest.getParsedCreatedTo(),
                                searchRequest.getParsedUpdatedFrom(),
                                searchRequest.getParsedUpdatedTo(),
                                searchRequest.getPage(),
                                searchRequest.getSize())
                                .flatMap(response -> toEntityResource(response, includeDeleted))
                                .collectList()
                                .flatMap(resources -> toCollectionResource(
                                                Flux.fromIterable(resources),
                                                includeDeleted,
                                                searchRequest))
                                .map(ResponseEntity::ok);
        }

        // ==================== UPDATE ====================

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

        @PostMapping("/transaction")
        public Mono<ResponseEntity<WarehouseDTO.Response>> processTransaction(
                        @Valid @RequestBody WarehouseDTO.InventoryTransactionRequest request) {
                log.info("POST /warehouse/management/transaction - {} for warehouse {}",
                                request.getType(), request.getWarehouseId());

                return managementService.processInventoryTransaction(request)
                                .map(ResponseEntity::ok);
        }

        @PatchMapping("/{warehouseId}/import")
        public Mono<ResponseEntity<WarehouseDTO.Response>> importInventory(
                        @PathVariable UUID warehouseId,
                        @RequestParam @Min(1) int quantity,
                        @RequestHeader("X-User-Id") String userId) {
                log.info("PATCH /warehouse/management/{}/import - quantity: {}", warehouseId, quantity);

                return managementService.importInventory(warehouseId, quantity, userId)
                                .map(ResponseEntity::ok);
        }

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

        @DeleteMapping("/{id}")
        public Mono<ResponseEntity<Void>> softDelete(
                        @PathVariable UUID id,
                        @RequestHeader("X-User-Id") String userId) {
                log.info("DELETE /warehouse/management/{}", id);

                return managementService.softDelete(id, userId)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
        }

        @PatchMapping("/{id}/restore")
        public Mono<ResponseEntity<WarehouseDTO.Response>> restore(
                        @PathVariable UUID id,
                        @RequestHeader("X-User-Id") String userId) {
                log.info("PATCH /warehouse/management/{}/restore", id);

                return managementService.restore(id, userId)
                                .map(ResponseEntity::ok);
        }

        @DeleteMapping("/{id}/permanent")
        public Mono<ResponseEntity<Void>> permanentDelete(@PathVariable UUID id) {
                log.warn("DELETE /warehouse/management/{}/permanent", id);

                return managementService.permanentDelete(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
        }

        @DeleteMapping("/batch")
        public Mono<ResponseEntity<Long>> batchSoftDelete(
                        @RequestBody Flux<UUID> ids,
                        @RequestHeader("X-User-Id") String userId) {
                log.info("DELETE /warehouse/management/batch");

                return managementService.batchSoftDelete(ids, userId)
                                .map(ResponseEntity::ok);
        }

        // ==================== HATEOAS RESOURCE METHODS ====================

        /**
         * Creates EntityModel for warehouse with self link
         */
        private Mono<EntityModel<WarehouseDTO.Response>> toEntityResource(WarehouseDTO.Response response,
                        boolean includeDeleted) {
                return createSelfLink(response.getId(), includeDeleted)
                                .map(selfLink -> EntityModel.of(response, selfLink));
        }

        /**
         * Creates EntityModel for warehouse with self and collection links
         */
        private Mono<EntityModel<WarehouseDTO.Response>> toEntityResourceWithCollection(WarehouseDTO.Response response,
                        boolean includeDeleted) {
                return Mono.zip(
                                createSelfLink(response.getId(), includeDeleted),
                                createCollectionLink(includeDeleted))
                                .map(tuple -> EntityModel.of(response, tuple.getT1(), tuple.getT2()));
        }

        /**
         * Creates EntityModel for product name search with self and details links
         */
        private Mono<EntityModel<WarehouseDTO.Response>> toEntityResourceWithDetails(WarehouseDTO.Response response,
                        String productName, boolean includeDeleted) {
                return Mono.zip(
                                createProductNameSelfLink(productName, includeDeleted),
                                createDetailsLink(response.getId(), includeDeleted))
                                .map(tuple -> EntityModel.of(response, tuple.getT1(), tuple.getT2()));
        }

        /**
         * Creates CollectionModel for filtered search with pagination
         */
        private Mono<CollectionModel<EntityModel<WarehouseDTO.Response>>> toCollectionResource(
                        Flux<EntityModel<WarehouseDTO.Response>> resources,
                        boolean includeDeleted, WarehouseSearchDTO.Request request) {

                return resources.collectList()
                                .flatMap(entityResources -> {
                                        CollectionModel<EntityModel<WarehouseDTO.Response>> collection = CollectionModel
                                                        .of(entityResources);

                                        return createFilteredSelfLink(includeDeleted, request)
                                                        .flatMap(selfLink -> {
                                                                collection.add(selfLink);
                                                                return addPaginationLinks(collection, includeDeleted,
                                                                                request);
                                                        });
                                });
        }

        /**
         * Creates simple CollectionModel for basic findAll
         */
        private Mono<CollectionModel<EntityModel<WarehouseDTO.Response>>> toSimpleCollectionResource(
                        Flux<EntityModel<WarehouseDTO.Response>> resources,
                        boolean includeDeleted, int page, int size) {

                return resources.collectList()
                                .flatMap(entityResources -> {
                                        CollectionModel<EntityModel<WarehouseDTO.Response>> collection = CollectionModel
                                                        .of(entityResources);

                                        return createSimpleSelfLink(includeDeleted, page, size)
                                                        .flatMap(selfLink -> {
                                                                collection.add(selfLink);
                                                                return addSimplePaginationLinks(collection,
                                                                                includeDeleted, page, size);
                                                        });
                                });
        }

        // ==================== LINK BUILDING METHODS ====================

        /**
         * Creates self link for warehouse by ID
         */
        private Mono<org.springframework.hateoas.Link> createSelfLink(UUID id, boolean includeDeleted) {
                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findById(id, includeDeleted))
                                .withSelfRel().toMono();
        }

        /**
         * Creates self link for product name search
         */
        private Mono<org.springframework.hateoas.Link> createProductNameSelfLink(String productName,
                        boolean includeDeleted) {
                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findByProductName(productName, includeDeleted))
                                .withSelfRel().toMono();
        }

        /**
         * Creates details link for warehouse by ID
         */
        private Mono<org.springframework.hateoas.Link> createDetailsLink(UUID id, boolean includeDeleted) {
                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findById(id, includeDeleted))
                                .withRel("details").toMono();
        }

        /**
         * Creates collection link for all warehouses
         */
        private Mono<org.springframework.hateoas.Link> createCollectionLink(boolean includeDeleted) {
                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findAll(includeDeleted, 0, 20))
                                .withRel("collection").toMono();
        }

        /**
         * Creates self link for filtered search
         */
        private Mono<org.springframework.hateoas.Link> createFilteredSelfLink(
                        boolean includeDeleted, WarehouseSearchDTO.Request request) {

                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findAllWithFilters(includeDeleted, request))
                                .withSelfRel().toMono();
        }

        /**
         * Creates self link for simple findAll
         */
        private Mono<org.springframework.hateoas.Link> createSimpleSelfLink(boolean includeDeleted, int page,
                        int size) {
                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findAll(includeDeleted, page, size))
                                .withSelfRel().toMono();
        }

        /**
         * Adds pagination links to filtered collection
         */
        private Mono<CollectionModel<EntityModel<WarehouseDTO.Response>>> addPaginationLinks(
                        CollectionModel<EntityModel<WarehouseDTO.Response>> collection,
                        boolean includeDeleted, WarehouseSearchDTO.Request request) {

                Mono<Void> previousLinkMono = Mono.empty();
                if (request.getPage() > 0) {
                        request.setPage(request.getPage() - 1);
                        previousLinkMono = WebFluxLinkBuilder.linkTo(
                                        WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                        .findAllWithFilters(includeDeleted, request))
                                        .withRel("previous").toMono().doOnNext(collection::add).then();
                }
                request.setPage(request.getPage() + 1);
                Mono<Void> nextLinkMono = WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findAllWithFilters(includeDeleted, request))
                                .withRel("next").toMono().doOnNext(collection::add).then();

                return Mono.when(previousLinkMono, nextLinkMono).thenReturn(collection);
        }

        /**
         * Adds pagination links to simple collection
         */
        private Mono<CollectionModel<EntityModel<WarehouseDTO.Response>>> addSimplePaginationLinks(
                        CollectionModel<EntityModel<WarehouseDTO.Response>> collection,
                        boolean includeDeleted, int page, int size) {

                Mono<Void> previousLinkMono = Mono.empty();
                if (page > 0) {
                        previousLinkMono = WebFluxLinkBuilder.linkTo(
                                        WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                        .findAll(includeDeleted, page - 1, size))
                                        .withRel("previous").toMono().doOnNext(collection::add).then();
                }

                Mono<Void> nextLinkMono = WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(WarehouseManagementController.class)
                                                .findAll(includeDeleted, page + 1, size))
                                .withRel("next").toMono().doOnNext(collection::add).then();

                return Mono.when(previousLinkMono, nextLinkMono).thenReturn(collection);
        }
}