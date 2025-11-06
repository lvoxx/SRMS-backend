package io.github.lvoxx.srms.warehouse.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

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

import io.github.lvoxx.srms.controllerhandler.model.InternalServerException;
import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseHistoryRepository;
import io.github.lvoxx.srms.warehouse.repositories.WarehouseRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Warehouse Count Service Tests")
@Tags({
    @Tag("Service"), @Tag("Mock"), @Tag("Message"), @Tag("Integrate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WarehouseCountServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseHistoryRepository warehouseHistoryRepository;

    @InjectMocks
    private WarehouseCountService countService;

    @Nested
    @DisplayName("Count All Warehouses Tests")
    class CountAllWarehousesTests {

        @Test
        @DisplayName("Should count all warehouses successfully")
        void shouldCountAllWarehousesSuccessfully() {
            // Arrange
            Long expectedCount = 100L;
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countAllWarehouses(false);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedCount, response.getCount());
                    assertEquals("Total warehouses", response.getDescription());
                })
                .verifyComplete();

            verify(warehouseRepository).countAll(false);
        }

        @Test
        @DisplayName("Should count all warehouses including deleted")
        void shouldCountAllWarehousesIncludingDeleted() {
            // Arrange
            Long expectedCount = 150L;
            when(warehouseRepository.countAll(true)).thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countAllWarehouses(true);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedCount, response.getCount());
                })
                .verifyComplete();

            verify(warehouseRepository).countAll(true);
        }

        @Test
        @DisplayName("Should return zero when no warehouses exist")
        void shouldReturnZeroWhenNoWarehousesExist() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(0L));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countAllWarehouses(false);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0L, response.getCount());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw InternalServerException when repository fails")
        void shouldThrowExceptionWhenRepositoryFails() {
            // Arrange
            when(warehouseRepository.countAll(false))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countAllWarehouses(false);

            // Assert
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof InternalServerException &&
                    throwable.getMessage().contains("Failed to count warehouses"))
                .verify();

            verify(warehouseRepository).countAll(false);
        }
    }

    @Nested
    @DisplayName("Count Below Minimum Tests")
    class CountBelowMinimumTests {

        @Test
        @DisplayName("Should count warehouses below minimum successfully")
        void shouldCountBelowMinimumSuccessfully() {
            // Arrange
            Long expectedCount = 25L;
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countBelowMinimum();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedCount, response.getCount());
                    assertEquals("Warehouses below minimum quantity", response.getDescription());
                })
                .verifyComplete();

            verify(warehouseRepository).countBelowMinimum();
        }

        @Test
        @DisplayName("Should return zero when no warehouses below minimum")
        void shouldReturnZeroWhenNoBelowMinimum() {
            // Arrange
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countBelowMinimum();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> assertEquals(0L, response.getCount()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw InternalServerException on error")
        void shouldThrowExceptionOnError() {
            // Arrange
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.error(new RuntimeException("Query error")));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countBelowMinimum();

            // Assert
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof InternalServerException &&
                    throwable.getMessage().contains("Failed to count warehouses below minimum"))
                .verify();
        }
    }

    @Nested
    @DisplayName("Count Out Of Stock Tests")
    class CountOutOfStockTests {

        @Test
        @DisplayName("Should count out of stock warehouses successfully")
        void shouldCountOutOfStockSuccessfully() {
            // Arrange
            Long expectedCount = 10L;
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countOutOfStock();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(expectedCount, response.getCount());
                    assertEquals("Out of stock warehouses", response.getDescription());
                })
                .verifyComplete();

            verify(warehouseRepository).countOutOfStock();
        }

        @Test
        @DisplayName("Should handle empty result")
        void shouldHandleEmptyResult() {
            // Arrange
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(countService.countOutOfStock())
                .assertNext(response -> assertEquals(0L, response.getCount()))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Count History Tests")
    class CountHistoryTests {

        @Test
        @DisplayName("Should count all history entries successfully")
        void shouldCountAllHistorySuccessfully() {
            // Arrange
            Long expectedCount = 500L;
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countAllHistory();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(expectedCount, response.getCount());
                    assertEquals("Total history entries", response.getDescription());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should count history by warehouse ID")
        void shouldCountHistoryByWarehouseId() {
            // Arrange
            UUID warehouseId = UUID.randomUUID();
            Long expectedCount = 50L;
            when(warehouseHistoryRepository.countByWarehouseId(warehouseId))
                .thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = 
                countService.countHistoryByWarehouseId(warehouseId);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(expectedCount, response.getCount());
                    assertTrue(response.getDescription().contains(warehouseId.toString()));
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should count history by type")
        void shouldCountHistoryByType() {
            // Arrange
            String type = "import";
            Long expectedCount = 200L;
            when(warehouseHistoryRepository.countByType(type))
                .thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = countService.countHistoryByType(type);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(expectedCount, response.getCount());
                    assertTrue(response.getDescription().contains(type));
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should count history by warehouse ID and type")
        void shouldCountHistoryByWarehouseIdAndType() {
            // Arrange
            UUID warehouseId = UUID.randomUUID();
            String type = "export";
            Long expectedCount = 75L;
            when(warehouseHistoryRepository.countByWarehouseIdAndType(warehouseId, type))
                .thenReturn(Mono.just(expectedCount));

            // Act
            Mono<WarehouseCountDTO.CountResponse> result = 
                countService.countHistoryByWarehouseIdAndType(warehouseId, type);

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(expectedCount, response.getCount());
                    assertTrue(response.getDescription().contains(warehouseId.toString()));
                    assertTrue(response.getDescription().contains(type));
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Get Warehouse Statistics Tests")
    class GetWarehouseStatisticsTests {

        @Test
        @DisplayName("Should get comprehensive statistics successfully")
        void shouldGetStatisticsSuccessfully() {
            // Arrange
            Long totalWarehouses = 100L;
            Long belowMinimum = 25L;
            Long outOfStock = 10L;
            Long totalHistory = 500L;

            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(totalWarehouses));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(belowMinimum));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(outOfStock));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(totalHistory));

            // Act
            Mono<WarehouseCountDTO.StatisticsResponse> result = 
                countService.getWarehouseStatistics();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(totalWarehouses, response.getTotalWarehouses());
                    assertEquals(belowMinimum, response.getBelowMinimum());
                    assertEquals(outOfStock, response.getOutOfStock());
                    assertEquals(totalHistory, response.getTotalHistoryEntries());
                    assertEquals(90L, response.getInStock()); // 100 - 10
                })
                .verifyComplete();

            verify(warehouseRepository).countAll(false);
            verify(warehouseRepository).countBelowMinimum();
            verify(warehouseRepository).countOutOfStock();
            verify(warehouseHistoryRepository).countAll();
        }

        @Test
        @DisplayName("Should handle zero values in statistics")
        void shouldHandleZeroValuesInStatistics() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(0L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(countService.getWarehouseStatistics())
                .assertNext(response -> {
                    assertEquals(0L, response.getTotalWarehouses());
                    assertEquals(0L, response.getBelowMinimum());
                    assertEquals(0L, response.getOutOfStock());
                    assertEquals(0L, response.getTotalHistoryEntries());
                    assertEquals(0L, response.getInStock());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should throw exception when one of the counts fails")
        void shouldThrowExceptionWhenCountFails() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(100L));
            when(warehouseRepository.countBelowMinimum())
                .thenReturn(Mono.error(new RuntimeException("Database error")));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(10L));
            when(warehouseHistoryRepository.countAll()).thenReturn(Mono.just(500L));

            // Act & Assert
            StepVerifier.create(countService.getWarehouseStatistics())
                .expectErrorMatches(throwable -> 
                    throwable instanceof InternalServerException &&
                    throwable.getMessage().contains("Failed to get warehouse statistics"))
                .verify();
        }
    }

    @Nested
    @DisplayName("Get Warehouse Health Metrics Tests")
    class GetWarehouseHealthMetricsTests {

        @Test
        @DisplayName("Should calculate health metrics correctly")
        void shouldCalculateHealthMetricsCorrectly() {
            // Arrange
            Long totalWarehouses = 100L;
            Long belowMinimum = 20L;
            Long outOfStock = 5L;

            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(totalWarehouses));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(belowMinimum));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(outOfStock));

            // Act
            Mono<WarehouseCountDTO.HealthMetricsResponse> result = 
                countService.getWarehouseHealthMetrics();

            // Assert
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(totalWarehouses, response.getTotalWarehouses());
                    assertEquals(belowMinimum, response.getBelowMinimum());
                    assertEquals(outOfStock, response.getOutOfStock());
                    assertEquals(20.0, response.getBelowMinimumPercentage(), 0.01);
                    assertEquals(5.0, response.getOutOfStockPercentage(), 0.01);
                    assertEquals(80.0, response.getHealthyPercentage(), 0.01);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle zero total warehouses")
        void shouldHandleZeroTotalWarehouses() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(0L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(countService.getWarehouseHealthMetrics())
                .assertNext(response -> {
                    assertEquals(0.0, response.getBelowMinimumPercentage(), 0.01);
                    assertEquals(0.0, response.getOutOfStockPercentage(), 0.01);
                    assertEquals(100.0, response.getHealthyPercentage(), 0.01);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should calculate 100% healthy when all stocks are good")
        void shouldCalculate100PercentHealthy() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(50L));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(0L));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(countService.getWarehouseHealthMetrics())
                .assertNext(response -> {
                    assertEquals(0.0, response.getBelowMinimumPercentage());
                    assertEquals(0.0, response.getOutOfStockPercentage());
                    assertEquals(100.0, response.getHealthyPercentage());
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should calculate critical health metrics")
        void shouldCalculateCriticalHealthMetrics() {
            // Arrange
            Long totalWarehouses = 100L;
            Long belowMinimum = 80L;
            Long outOfStock = 40L;

            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(totalWarehouses));
            when(warehouseRepository.countBelowMinimum()).thenReturn(Mono.just(belowMinimum));
            when(warehouseRepository.countOutOfStock()).thenReturn(Mono.just(outOfStock));

            // Act & Assert
            StepVerifier.create(countService.getWarehouseHealthMetrics())
                .assertNext(response -> {
                    assertEquals(80.0, response.getBelowMinimumPercentage());
                    assertEquals(40.0, response.getOutOfStockPercentage());
                    assertEquals(20.0, response.getHealthyPercentage());
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null pointer exception")
        void shouldHandleNullPointerException() {
            // Arrange
            when(warehouseRepository.countAll(false))
                .thenReturn(Mono.error(new NullPointerException("Null value")));

            // Act & Assert
            StepVerifier.create(countService.countAllWarehouses(false))
                .expectError(InternalServerException.class)
                .verify();
        }

        @Test
        @DisplayName("Should handle database connection timeout")
        void shouldHandleDatabaseTimeout() {
            // Arrange
            when(warehouseHistoryRepository.countAll())
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

            // Act & Assert
            StepVerifier.create(countService.countAllHistory())
                .expectErrorMatches(throwable -> 
                    throwable instanceof InternalServerException &&
                    throwable.getMessage().contains("Connection timeout"))
                .verify();
        }
    }

    @Nested
    @DisplayName("Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should verify repository is called exactly once")
        void shouldVerifyRepositoryCalledOnce() {
            // Arrange
            when(warehouseRepository.countAll(false)).thenReturn(Mono.just(100L));

            // Act
            countService.countAllWarehouses(false).block();

            // Assert
            verify(warehouseRepository, times(1)).countAll(false);
            verifyNoMoreInteractions(warehouseRepository);
        }

        @Test
        @DisplayName("Should verify no interaction when error occurs early")
        void shouldVerifyNoInteractionOnEarlyError() {
            // Arrange
            when(warehouseRepository.countAll(false))
                .thenReturn(Mono.error(new RuntimeException("Early error")));

            // Act
            try {
                countService.countAllWarehouses(false).block();
            } catch (Exception e) {
                // Expected
            }

            // Assert
            verify(warehouseRepository).countAll(false);
            verifyNoInteractions(warehouseHistoryRepository);
        }
    }
}