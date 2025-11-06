package io.github.lvoxx.srms.warehouse.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import io.github.lvoxx.srms.warehouse.mapper.WarehouseMapper;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Warehouse Statistic Service Tests")
@Tags({
    @Tag("Service"), @Tag("Mock"), @Tag("Message"), @Tag("Integrate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WarehouseStatisticServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseHistoryRepository warehouseHistoryRepository;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private WarehouseStatisticService statisticService;

    private UUID testWarehouseId;
    private Warehouse testWarehouse;
    private WarehouseDTO.Response testWarehouseResponse;

    @BeforeEach
    void setUp() {
        testWarehouseId = UUID.randomUUID();
        
        testWarehouse = Warehouse.builder()
            .id(testWarehouseId)
            .productName("Test Product")
            .quantity(100)
            .minQuantity(50)
            .isDeleted(false)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        testWarehouseResponse = WarehouseDTO.Response.builder()
            .id(testWarehouseId)
            .productName("Test Product")
            .quantity(100)
            .minQuantity(50)
            .build();
    }

    @Nested
    @DisplayName("Import/Export Statistics Tests")
    class ImportExportStatisticsTests {

        @Test
        @DisplayName("Should get total import quantity successfully")
        void shouldGetTotalImportQuantitySuccessfully() {
            // Arrange
            Long expectedQuantity = 1000L;
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(expectedQuantity));

            // Act
            Mono<WarehouseStatisticDTO.QuantityResponse> result = 
                statisticService.getTotalImportQuantity(testWarehouseId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(testWarehouseId, response.getWarehouseId());
                    assertEquals(expectedQuantity, response.getQuantity());
                    assertEquals("IMPORT", response.getType());
                    assertEquals("Total imported quantity", response.getDescription());
                })
                .verifyComplete();

            verify(warehouseHistoryRepository).getTotalImportQuantity(testWarehouseId);
        }

        @Test
        @DisplayName("Should get total export quantity successfully")
        void shouldGetTotalExportQuantitySuccessfully() {
            // Arrange
            Long expectedQuantity = 500L;
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(expectedQuantity));

            // Act
            Mono<WarehouseStatisticDTO.QuantityResponse> result = 
                statisticService.getTotalExportQuantity(testWarehouseId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(testWarehouseId, response.getWarehouseId());
                    assertEquals(expectedQuantity, response.getQuantity());
                    assertEquals("EXPORT", response.getType());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get quantity by type and date range")
        void shouldGetQuantityByTypeAndDateRange() {
            // Arrange
            String type = "import";
            OffsetDateTime from = OffsetDateTime.now().minusDays(7);
            OffsetDateTime to = OffsetDateTime.now();
            Long expectedQuantity = 300L;

            when(warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                testWarehouseId, type, from, to))
                .thenReturn(Mono.just(expectedQuantity));

            // Act
            Mono<WarehouseStatisticDTO.QuantityResponse> result = 
                statisticService.getQuantityByTypeAndDateRange(testWarehouseId, type, from, to);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(testWarehouseId, response.getWarehouseId());
                    assertEquals(expectedQuantity, response.getQuantity());
                    assertEquals("IMPORT", response.getType());
                    assertEquals(from, response.getFromDate());
                    assertEquals(to, response.getToDate());
                    assertTrue(response.getDescription().contains("IMPORT"));
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should calculate import/export balance correctly")
        void shouldCalculateBalanceCorrectly() {
            // Arrange
            Long totalImport = 1000L;
            Long totalExport = 600L;

            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalImport));
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalExport));

            // Act
            Mono<WarehouseStatisticDTO.BalanceResponse> result = 
                statisticService.getImportExportBalance(testWarehouseId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(testWarehouseId, response.getWarehouseId());
                    assertEquals(totalImport, response.getTotalImport());
                    assertEquals(totalExport, response.getTotalExport());
                    assertEquals(400L, response.getBalance()); // 1000 - 600
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle negative balance")
        void shouldHandleNegativeBalance() {
            // Arrange
            Long totalImport = 500L;
            Long totalExport = 800L;

            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalImport));
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalExport));

            // Act & Assert
            StepVerifier.create(statisticService.getImportExportBalance(testWarehouseId))
                .assertNext(response -> assertEquals(-300L, response.getBalance()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw exception when import query fails")
        void shouldThrowExceptionWhenImportQueryFails() {
            // Arrange
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

            // Act & Assert
            StepVerifier.create(statisticService.getTotalImportQuantity(testWarehouseId))
                .expectError(InternalServerException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Alert Management Tests")
    class AlertManagementTests {

        @Test
        @DisplayName("Should get products below minimum")
        void shouldGetProductsBelowMinimum() {
            // Arrange
            Warehouse warehouse1 = createWarehouse("Product A", 30, 50);
            Warehouse warehouse2 = createWarehouse("Product B", 20, 50);
            
            when(warehouseRepository.findProductsBelowMinimum(any(Pageable.class)))
                .thenReturn(Flux.just(warehouse1, warehouse2));
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.just(2L));

            // Act
            Mono<WarehouseStatisticDTO.AlertListResponse> result = 
                statisticService.getProductsBelowMinimum(0, 10);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.getItems().size());
                    assertEquals(2L, response.getTotalItems());
                    assertEquals("BELOW_MINIMUM", response.getAlertType());
                    assertEquals(0, response.getPage());
                    assertEquals(10, response.getSize());
                    
                    // Verify first item
                    WarehouseStatisticDTO.AlertItem item1 = response.getItems().get(0);
                    assertEquals("Product A", item1.getProductName());
                    assertEquals(30, item1.getCurrentQuantity());
                    assertEquals(50, item1.getMinQuantity());
                    assertEquals(20, item1.getDeficit());
                    assertEquals("WARNING", item1.getSeverity());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get out of stock products")
        void shouldGetOutOfStockProducts() {
            // Arrange
            Warehouse warehouse = createWarehouse("Out of Stock Product", 0, 50);
            
            when(warehouseRepository.findOutOfStock(any(Pageable.class)))
                .thenReturn(Flux.just(warehouse));
            when(warehouseRepository.countOutOfStock())
                .thenReturn(Mono.just(1L));

            // Act
            Mono<WarehouseStatisticDTO.AlertListResponse> result = 
                statisticService.getOutOfStockProducts(0, 10);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(1, response.getItems().size());
                    assertEquals("OUT_OF_STOCK", response.getAlertType());
                    
                    WarehouseStatisticDTO.AlertItem item = response.getItems().get(0);
                    assertEquals(0, item.getCurrentQuantity());
                    assertEquals("CRITICAL", item.getSeverity());
                    assertTrue(item.getMessage().contains("out of stock"));
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get all warehouse alerts")
        void shouldGetAllWarehouseAlerts() {
            // Arrange
            Warehouse belowMin = createWarehouse("Below Min", 30, 50);
            Warehouse outOfStock = createWarehouse("Out of Stock", 0, 50);
            
            when(warehouseRepository.findProductsBelowMinimum(any(Pageable.class)))
                .thenReturn(Flux.just(belowMin));
            when(warehouseRepository.findOutOfStock(any(Pageable.class)))
                .thenReturn(Flux.just(outOfStock));
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.just(1L));
            when(warehouseRepository.countOutOfStock())
                .thenReturn(Mono.just(1L));

            // Act
            Mono<WarehouseStatisticDTO.AlertListResponse> result = 
                statisticService.getAllWarehouseAlerts(0, 20);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(2, response.getItems().size());
                    assertEquals(2L, response.getTotalItems());
                    assertEquals("ALL_ALERTS", response.getAlertType());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle empty alerts")
        void shouldHandleEmptyAlerts() {
            // Arrange
            when(warehouseRepository.findProductsBelowMinimum(any(Pageable.class)))
                .thenReturn(Flux.empty());
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(statisticService.getProductsBelowMinimum(0, 10))
                .assertNext(response -> {
                    assertTrue(response.getItems().isEmpty());
                    assertEquals(0L, response.getTotalItems());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should determine correct severity levels")
        void shouldDetermineCorrectSeverityLevels() {
            // Arrange
            Warehouse critical = createWarehouse("Critical", 0, 50);
            Warehouse warning = createWarehouse("Warning", 30, 50);
            
            when(warehouseRepository.findProductsBelowMinimum(any(Pageable.class)))
                .thenReturn(Flux.just(critical, warning));
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.just(2L));

            // Act & Assert
            StepVerifier.create(statisticService.getProductsBelowMinimum(0, 10))
                .assertNext(response -> {
                    assertEquals("CRITICAL", response.getItems().get(0).getSeverity());
                    assertEquals("WARNING", response.getItems().get(1).getSeverity());
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Dashboard Statistics Tests")
    class DashboardStatisticsTests {

        @Test
        @DisplayName("Should get comprehensive dashboard statistics")
        void shouldGetComprehensiveDashboardStatistics() {
            // Arrange
            Long totalWarehouses = 100L;
            Long belowMinimum = 20L;
            Long outOfStock = 5L;
            Long totalHistory = 500L;
            Long totalImports = 300L;
            Long totalExports = 200L;

            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(totalWarehouses));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(belowMinimum));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(outOfStock));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(totalHistory));
            when(warehouseHistoryRepository.countByType("import")).thenReturn(Mono.just(totalImports));
            when(warehouseHistoryRepository.countByType("export")).thenReturn(Mono.just(totalExports));

            // Act
            Mono<WarehouseStatisticDTO.DashboardResponse> result = 
                statisticService.getDashboardStatistics();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(totalWarehouses, response.getTotalWarehouses());
                    assertEquals(80L, response.getHealthyWarehouses()); // 100 - 20
                    assertEquals(belowMinimum, response.getBelowMinimum());
                    assertEquals(outOfStock, response.getOutOfStock());
                    assertEquals(80.0, response.getHealthPercentage(), 0.01);
                    assertEquals(totalHistory, response.getTotalTransactions());
                    assertEquals(totalImports, response.getTotalImportTransactions());
                    assertEquals(totalExports, response.getTotalExportTransactions());
                    assertNotNull(response.getTimestamp());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should calculate 100% health when all warehouses healthy")
        void shouldCalculate100PercentHealth() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(50L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(100L));
            when(warehouseHistoryRepository.countByType("import")).thenReturn(Mono.just(60L));
            when(warehouseHistoryRepository.countByType("export")).thenReturn(Mono.just(40L));

            // Act & Assert
            StepVerifier.create(statisticService.getDashboardStatistics())
                .assertNext(response -> {
                    assertEquals(50L, response.getHealthyWarehouses());
                    assertEquals(100.0, response.getHealthPercentage());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle zero warehouses in dashboard")
        void shouldHandleZeroWarehousesInDashboard() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(0L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(0L));
            when(warehouseHistoryRepository.countByType("import")).thenReturn(Mono.just(0L));
            when(warehouseHistoryRepository.countByType("export")).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(statisticService.getDashboardStatistics())
                .assertNext(response -> {
                    assertEquals(0L, response.getTotalWarehouses());
                    assertEquals(100.0, response.getHealthPercentage());
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Warehouse Details Tests")
    class WarehouseDetailsTests {

        @Test
        @DisplayName("Should get warehouse details successfully")
        void shouldGetWarehouseDetailsSuccessfully() {
            // Arrange
            Long totalImport = 1000L;
            Long totalExport = 600L;
            Long transactionCount = 50L;

            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalImport));
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(totalExport));
            when(warehouseHistoryRepository.countByWarehouseId(testWarehouseId))
                .thenReturn(Mono.just(transactionCount));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testWarehouseResponse);

            // Act
            Mono<WarehouseStatisticDTO.WarehouseDetailsResponse> result = 
                statisticService.getWarehouseDetails(testWarehouseId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(testWarehouseResponse, response.getWarehouse());
                    assertEquals(totalImport, response.getTotalImport());
                    assertEquals(totalExport, response.getTotalExport());
                    assertEquals(400L, response.getBalance()); // 1000 - 600
                    assertEquals(transactionCount, response.getTransactionCount());
                    assertFalse(response.getIsBelowMinimum());
                    assertFalse(response.getIsOutOfStock());
                    assertNotNull(response.getTimestamp());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should detect warehouse below minimum")
        void shouldDetectWarehouseBelowMinimum() {
            // Arrange
            Warehouse belowMinWarehouse = createWarehouse("Low Stock", 30, 50);
            
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(belowMinWarehouse));
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(100L));
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(70L));
            when(warehouseHistoryRepository.countByWarehouseId(testWarehouseId))
                .thenReturn(Mono.just(10L));
            when(warehouseMapper.toResponse(belowMinWarehouse))
                .thenReturn(testWarehouseResponse);

            // Act & Assert
            StepVerifier.create(statisticService.getWarehouseDetails(testWarehouseId))
                .assertNext(response -> assertTrue(response.getIsBelowMinimum()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw NotFoundException when warehouse not found")
        void shouldThrowNotFoundExceptionWhenWarehouseNotFound() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(statisticService.getWarehouseDetails(testWarehouseId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof NotFoundException &&
                    throwable.getMessage().contains(testWarehouseId.toString()))
                .verify();
        }
    }

    @Nested
    @DisplayName("Time Based Statistics Tests")
    class TimeBasedStatisticsTests {

        @Test
        @DisplayName("Should get time-based statistics successfully")
        void shouldGetTimeBasedStatisticsSuccessfully() {
            // Arrange
            OffsetDateTime from = OffsetDateTime.now().minusDays(7);
            OffsetDateTime to = OffsetDateTime.now();
            Long importQuantity = 500L;
            Long exportQuantity = 300L;

            when(warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                testWarehouseId, "import", from, to))
                .thenReturn(Mono.just(importQuantity));
            when(warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                testWarehouseId, "export", from, to))
                .thenReturn(Mono.just(exportQuantity));

            // Act
            Mono<WarehouseStatisticDTO.TimeBasedStatisticsResponse> result = 
                statisticService.getTimeBasedStatistics(testWarehouseId, from, to);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(testWarehouseId, response.getWarehouseId());
                    assertEquals(from, response.getFromDate());
                    assertEquals(to, response.getToDate());
                    assertEquals(importQuantity, response.getImportQuantity());
                    assertEquals(exportQuantity, response.getExportQuantity());
                    assertEquals(200L, response.getNetChange()); // 500 - 300
                    assertNotNull(response.getTimestamp());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle negative net change")
        void shouldHandleNegativeNetChange() {
            // Arrange
            OffsetDateTime from = OffsetDateTime.now().minusDays(1);
            OffsetDateTime to = OffsetDateTime.now();

            when(warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                testWarehouseId, "import", from, to))
                .thenReturn(Mono.just(100L));
            when(warehouseHistoryRepository.getQuantityByTypeAndDateRange(
                testWarehouseId, "export", from, to))
                .thenReturn(Mono.just(200L));

            // Act & Assert
            StepVerifier.create(statisticService.getTimeBasedStatistics(
                testWarehouseId, from, to))
                .assertNext(response -> assertEquals(-100L, response.getNetChange()))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository error gracefully")
        void shouldHandleRepositoryError() {
            // Arrange
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

            // Act & Assert
            StepVerifier.create(statisticService.getTotalImportQuantity(testWarehouseId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof InternalServerException &&
                    throwable.getMessage().contains("Failed to get total import quantity"))
                .verify();
        }

        @Test
        @DisplayName("Should propagate NotFoundException")
        void shouldPropagateNotFoundException() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(statisticService.getWarehouseDetails(testWarehouseId))
                .expectError(NotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Stream Tests")
    class StreamTests {

        @Test
        @DisplayName("Should create dashboard statistics stream")
        void shouldCreateDashboardStatisticsStream() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(100L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(20L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(5L));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(500L));
            when(warehouseHistoryRepository.countByType("import")).thenReturn(Mono.just(300L));
            when(warehouseHistoryRepository.countByType("export")).thenReturn(Mono.just(200L));

            // Act
            Flux<WarehouseStatisticDTO.DashboardResponse> stream = 
                statisticService.streamDashboardStatistics();

            // Assert
            StepVerifier.create(stream.take(2))
                .expectNextCount(2)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should create warehouse details stream")
        void shouldCreateWarehouseDetailsStream() {
            // Arrange
            when(warehouseRepository.findById(testWarehouseId, false))
                .thenReturn(Mono.just(testWarehouse));
            when(warehouseHistoryRepository.getTotalImportQuantity(testWarehouseId))
                .thenReturn(Mono.just(1000L));
            when(warehouseHistoryRepository.getTotalExportQuantity(testWarehouseId))
                .thenReturn(Mono.just(600L));
            when(warehouseHistoryRepository.countByWarehouseId(testWarehouseId))
                .thenReturn(Mono.just(50L));
            when(warehouseMapper.toResponse(testWarehouse))
                .thenReturn(testWarehouseResponse);

            // Act
            Flux<WarehouseStatisticDTO.WarehouseDetailsResponse> stream = 
                statisticService.streamWarehouseDetails(testWarehouseId);

            // Assert
            StepVerifier.create(stream.take(1))
                .expectNextCount(1)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle errors in stream gracefully")
        void shouldHandleErrorsInStreamGracefully() {
            // Arrange
            when(warehouseRepository.countAll(false))
                .thenReturn(Mono.error(new RuntimeException("Stream error")));

            // Act
            Flux<WarehouseStatisticDTO.DashboardResponse> stream = 
                statisticService.streamDashboardStatistics();

            // Assert - Stream should complete without errors
            StepVerifier.create(stream.take(1))
                .expectComplete()
                .verify();
        }
    }

    // Helper method
    private Warehouse createWarehouse(String name, int quantity, int minQuantity) {
        return Warehouse.builder()
            .id(UUID.randomUUID())
            .productName(name)
            .quantity(quantity)
            .minQuantity(minQuantity)
            .isDeleted(false)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }
}