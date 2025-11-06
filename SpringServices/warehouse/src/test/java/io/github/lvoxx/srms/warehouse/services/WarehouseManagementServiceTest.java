package io.github.lvoxx.srms.warehouse.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import io.github.lvoxx.srms.controllerhandler.model.ConflictException;
import io.github.lvoxx.srms.controllerhandler.model.DataPersistantException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.controllerhandler.model.ValidationException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.mapper.WarehouseMapper;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Warehouse Management Service Tests")
@Tags({
    @Tag("Service"), @Tag("Mock"), @Tag("CRUD"), @Tag("Integrate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WarehouseManagementServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseHistoryRepository warehouseHistoryRepository;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private WarehouseManagementService managementService;

    private String testUserId;
    private UUID testWarehouseId;
    private Warehouse testWarehouse;
    private WarehouseDTO.Request testRequest;
    private WarehouseDTO.Response testResponse;
    private WarehouseDTO.UpdateRequest testUpdateRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testWarehouseId = UUID.randomUUID();

        testWarehouse = Warehouse.builder()
            .id(testWarehouseId)
            .productName("Test Product")
            .quantity(100)
            .minQuantity(50)
            .isDeleted(false)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .lastUpdatedBy(testUserId.toString())
            .build();

        testRequest = WarehouseDTO.Request.builder()
            .productName("Test Product")
            .quantity(100)
            .minQuantity(50)
            .build();

        testUpdateRequest = WarehouseDTO.UpdateRequest.builder()
            .productName("Updated Product")
            .minQuantity(60)
            .build();

        testResponse = WarehouseDTO.Response.builder()
            .id(testWarehouseId)
            .productName("Test Product")
            .quantity(100)
            .minQuantity(50)
            .build();
    }

    @Nested
    @DisplayName("Create Warehouse Tests")
    class CreateWarehouseTests {

        @Test
        @DisplayName("Should create warehouse successfully")
        void shouldCreateWarehouseSuccessfully() {
            // Arrange
            when(warehouseRepository.findByProductName(anyString(), eq(false)))
                .thenReturn(Mono.empty());
            when(warehouseMapper.toEntity(testRequest))
                .thenReturn(testWarehouse);
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testResponse);

            // Act
            Mono<WarehouseDTO.Response> result = 
                managementService.createWarehouse(testRequest, testUserId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(testWarehouseId, response.getId());
                    assertEquals("Test Product", response.getProductName());
                })
                .verifyComplete();

            verify(warehouseRepository).save(any(Warehouse.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when product name exists")
        void shouldThrowConflictExceptionWhenProductNameExists() {
            // Arrange
            when(warehouseRepository.findByProductName(testRequest.getProductName(), false))
                .thenReturn(Mono.just(testWarehouse));

            // Act & Assert
            StepVerifier.create(managementService.createWarehouse(testRequest, testUserId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof ConflictException &&
                    throwable.getMessage().contains("already exists"))
                .verify();

            verify(warehouseRepository, never()).save(any(Warehouse.class));
        }

        @Test
        @DisplayName("Should throw DataPersistantException when save fails")
        void shouldThrowExceptionWhenSaveFails() {
            // Arrange
            when(warehouseRepository.findByProductName(anyString(), eq(false)))
                .thenReturn(Mono.empty());
            when(warehouseMapper.toEntity(testRequest))
                .thenReturn(testWarehouse);
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

            // Act & Assert
            StepVerifier.create(managementService.createWarehouse(testRequest, testUserId))
                .expectError(DataPersistantException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Read Warehouse Tests")
    class ReadWarehouseTests {

        @Test
        @DisplayName("Should find warehouse by ID successfully")
        void shouldFindWarehouseByIdSuccessfully() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testResponse);

            // Act
            Mono<WarehouseDTO.Response> result = 
                managementService.findById(testWarehouseId, false);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(testWarehouseId, response.getId());
                    assertEquals("Test Product", response.getProductName());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw NotFoundException when warehouse not found")
        void shouldThrowNotFoundExceptionWhenNotFound() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(managementService.findById(testWarehouseId, false))
                .expectErrorMatches(throwable -> 
                    throwable instanceof NotFoundException &&
                    throwable.getMessage().contains(testWarehouseId.toString()))
                .verify();
        }

        @Test
        @DisplayName("Should find warehouse by product name")
        void shouldFindWarehouseByProductName() {
            // Arrange
            String productName = "Test Product";
            when(warehouseRepository.findByProductName(productName, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testResponse);

            // Act & Assert
            StepVerifier.create(managementService.findByProductName(productName, false))
                .assertNext(response -> assertEquals(productName, response.getProductName()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should find all warehouses with pagination")
        void shouldFindAllWarehousesWithPagination() {
            // Arrange
            Warehouse warehouse2 = Warehouse.builder()
                .id(UUID.randomUUID())
                .productName("Product 2")
                .quantity(200)
                .minQuantity(100)
                .isDeleted(false)
                .build();

            when(warehouseRepository.findAll(eq(false), any(Pageable.class)))
                .thenReturn(Flux.just(testWarehouse, warehouse2));
            when(warehouseMapper.toResponse(any(Warehouse.class)))
                .thenReturn(testResponse);

            // Act
            Flux<WarehouseDTO.Response> result = 
                managementService.findAll(false, 0, 10);

            // Assert
            StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Update Warehouse Tests")
    class UpdateWarehouseTests {

        @Test
        @DisplayName("Should update warehouse successfully")
        void shouldUpdateWarehouseSuccessfully() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.findByProductName(
                testUpdateRequest.getProductName(), false))
                .thenReturn(Mono.empty());
            when(warehouseRepository.updateWarehouse(
                eq(testWarehouseId), anyString(), anyInt(), any(), any(), any()))
                .thenReturn(Mono.just(1));
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testResponse);

            // Act
            Mono<WarehouseDTO.Response> result = 
                managementService.updateWarehouse(testWarehouseId, testUpdateRequest, testUserId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

            verify(warehouseRepository).updateWarehouse(
                eq(testWarehouseId), anyString(), anyInt(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating non-existent warehouse")
        void shouldThrowNotFoundWhenUpdatingNonExistent() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(
                managementService.updateWarehouse(testWarehouseId, testUpdateRequest, testUserId))
                .expectError(NotFoundException.class)
                .verify();

            verify(warehouseRepository, never()).updateWarehouse(
                any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw ConflictException when new product name exists")
        void shouldThrowConflictWhenNewNameExists() {
            // Arrange
            Warehouse anotherWarehouse = Warehouse.builder()
                .id(UUID.randomUUID())
                .productName("Updated Product")
                .build();

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.findByProductName(
                testUpdateRequest.getProductName(), false))
                .thenReturn(Mono.just(anotherWarehouse));

            // Act & Assert
            StepVerifier.create(
                managementService.updateWarehouse(testWarehouseId, testUpdateRequest, testUserId))
                .expectError(ConflictException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Inventory Transaction Tests")
    class InventoryTransactionTests {

        @Test
        @DisplayName("Should import inventory successfully")
        void shouldImportInventorySuccessfully() {
            // Arrange
            int importQuantity = 50;
            WarehouseDTO.InventoryTransactionRequest request = 
                WarehouseDTO.InventoryTransactionRequest.builder()
                    .warehouseId(testWarehouseId)
                    .quantity(importQuantity)
                    .type(WarehouseHistory.HistoryType.IMPORT)
                    .updatedBy(testUserId.toString())
                    .build();

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toHistoryEntity(any()))
                .thenReturn(new WarehouseHistory());
            when(warehouseHistoryRepository.save(any(WarehouseHistory.class)))
                .thenReturn(Mono.just(new WarehouseHistory()));
            when(warehouseMapper.toResponse(any(Warehouse.class)))
                .thenReturn(testResponse);

            // Act
            Mono<WarehouseDTO.Response> result = 
                managementService.processInventoryTransaction(request);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

            verify(warehouseRepository).save(any(Warehouse.class));
            verify(warehouseHistoryRepository).save(any(WarehouseHistory.class));
        }

        @Test
        @DisplayName("Should export inventory successfully")
        void shouldExportInventorySuccessfully() {
            // Arrange
            int exportQuantity = 30;
            WarehouseDTO.InventoryTransactionRequest request = 
                WarehouseDTO.InventoryTransactionRequest.builder()
                    .warehouseId(testWarehouseId)
                    .quantity(exportQuantity)
                    .type(WarehouseHistory.HistoryType.EXPORT)
                    .updatedBy(testUserId.toString())
                    .build();

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toHistoryEntity(any()))
                .thenReturn(new WarehouseHistory());
            when(warehouseHistoryRepository.save(any(WarehouseHistory.class)))
                .thenReturn(Mono.just(new WarehouseHistory()));
            when(warehouseMapper.toResponse(any(Warehouse.class)))
                .thenReturn(testResponse);

            // Act & Assert
            StepVerifier.create(managementService.processInventoryTransaction(request))
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw ValidationException when exporting more than available")
        void shouldThrowValidationExceptionWhenExportingTooMuch() {
            // Arrange
            int exportQuantity = 150; // More than available (100)
            WarehouseDTO.InventoryTransactionRequest request = 
                WarehouseDTO.InventoryTransactionRequest.builder()
                    .warehouseId(testWarehouseId)
                    .quantity(exportQuantity)
                    .type(WarehouseHistory.HistoryType.EXPORT)
                    .updatedBy(testUserId.toString())
                    .build();

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));

            // Act & Assert
            StepVerifier.create(managementService.processInventoryTransaction(request))
                .expectErrorMatches(throwable -> 
                    throwable instanceof ValidationException &&
                    throwable.getMessage().contains("Insufficient inventory"))
                .verify();

            verify(warehouseRepository, never()).save(any(Warehouse.class));
        }

        @Test
        @DisplayName("Should calculate correct quantity after import")
        void shouldCalculateCorrectQuantityAfterImport() {
            // Arrange
            int initialQuantity = 100;
            int importQuantity = 50;
            
            Warehouse updatedWarehouse = testWarehouse.toBuilder()
                .quantity(initialQuantity + importQuantity)
                .build();

            WarehouseDTO.InventoryTransactionRequest request = 
                WarehouseDTO.InventoryTransactionRequest.builder()
                    .warehouseId(testWarehouseId)
                    .quantity(importQuantity)
                    .type(WarehouseHistory.HistoryType.IMPORT)
                    .updatedBy(testUserId.toString())
                    .build();

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> {
                    Warehouse saved = invocation.getArgument(0);
                    assertEquals(150, saved.getQuantity());
                    return Mono.just(saved);
                });
            when(warehouseMapper.toHistoryEntity(any()))
                .thenReturn(new WarehouseHistory());
            when(warehouseHistoryRepository.save(any(WarehouseHistory.class)))
                .thenReturn(Mono.just(new WarehouseHistory()));
            when(warehouseMapper.toResponse(any(Warehouse.class)))
                .thenReturn(testResponse);

            // Act & Assert
            StepVerifier.create(managementService.processInventoryTransaction(request))
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Delete Warehouse Tests")
    class DeleteWarehouseTests {

        @Test
        @DisplayName("Should soft delete warehouse successfully")
        void shouldSoftDeleteWarehouseSuccessfully() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.softDelete(eq(testWarehouseId), any(), any()))
                .thenReturn(Mono.just(1));

            // Act
            Mono<Void> result = managementService.softDelete(testWarehouseId, testUserId);

            // Assert
            StepVerifier.create(result)
                .verifyComplete();

            verify(warehouseRepository).softDelete(eq(testWarehouseId), any(), any());
        }

        @Test
        @DisplayName("Should throw NotFoundException when deleting non-existent warehouse")
        void shouldThrowNotFoundWhenDeletingNonExistent() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(managementService.softDelete(testWarehouseId, testUserId))
                .expectError(NotFoundException.class)
                .verify();

            verify(warehouseRepository, never()).softDelete(any(), any(), any());
        }

        @Test
        @DisplayName("Should restore deleted warehouse successfully")
        void shouldRestoreDeletedWarehouseSuccessfully() {
            // Arrange
            Warehouse deletedWarehouse = testWarehouse.toBuilder()
                .isDeleted(true)
                .build();

            when(warehouseRepository.findById(testWarehouseId, true))
                .thenReturn(Mono.just(deletedWarehouse));
            when(warehouseRepository.restore(eq(testWarehouseId), any(), any()))
                .thenReturn(Mono.just(1));
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testResponse);

            // Act
            Mono<WarehouseDTO.Response> result = 
                managementService.restore(testWarehouseId, testUserId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

            verify(warehouseRepository).restore(eq(testWarehouseId), any(), any());
        }

        @Test
        @DisplayName("Should throw ConflictException when restoring non-deleted warehouse")
        void shouldThrowConflictWhenRestoringNonDeleted() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, true))
                .thenReturn(Mono.just(testWarehouse));

            // Act & Assert
            StepVerifier.create(managementService.restore(testWarehouseId, testUserId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof ConflictException &&
                    throwable.getMessage().contains("not deleted"))
                .verify();
        }

        @Test
        @DisplayName("Should permanently delete warehouse")
        void shouldPermanentlyDeleteWarehouse() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, true))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.deleteById(testWarehouseId))
                .thenReturn(Mono.empty());

            // Act
            Mono<Void> result = managementService.permanentDelete(testWarehouseId);

            // Assert
            StepVerifier.create(result)
                .verifyComplete();

            verify(warehouseRepository).deleteById(testWarehouseId);
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should batch create warehouses")
        void shouldBatchCreateWarehouses() {
            // Arrange
            WarehouseDTO.Request request1 = WarehouseDTO.Request.builder()
                .productName("Product 1")
                .quantity(100)
                .minQuantity(50)
                .build();
            
            WarehouseDTO.Request request2 = WarehouseDTO.Request.builder()
                .productName("Product 2")
                .quantity(200)
                .minQuantity(100)
                .build();

            when(warehouseRepository.findByProductName(anyString(), eq(false)))
                .thenReturn(Mono.empty());
            when(warehouseMapper.toEntity(any()))
                .thenReturn(testWarehouse);
            when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseMapper.toResponse(any(Warehouse.class)))
                .thenReturn(testResponse);

            // Act
            Flux<WarehouseDTO.Response> result = 
                managementService.batchCreate(Flux.just(request1, request2), testUserId);

            // Assert
            StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

            verify(warehouseRepository, times(2)).save(any(Warehouse.class));
        }

        @Test
        @DisplayName("Should batch soft delete warehouses")
        void shouldBatchSoftDeleteWarehouses() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            when(warehouseRepository.findById(any(UUID.class), eq(false)))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseRepository.softDelete(any(UUID.class), any(), any()))
                .thenReturn(Mono.just(1));

            // Act
            Mono<Long> result = 
                managementService.batchSoftDelete(Flux.just(id1, id2), testUserId);

            // Assert
            StepVerifier.create(result)
                .assertNext(count -> assertEquals(2L, count))
                .verifyComplete();
        }
    }
}