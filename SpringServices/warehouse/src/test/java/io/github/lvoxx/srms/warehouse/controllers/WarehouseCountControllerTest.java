package io.github.lvoxx.srms.warehouse.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.warehouse.dto.WarehouseCountDTO;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO.Response;
import io.github.lvoxx.srms.warehouse.helper.MinimalWebFluxTest;
import io.github.lvoxx.srms.warehouse.services.WarehouseCountService;
import reactor.core.publisher.Mono;

@DisplayName("Warehouse Count Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@MinimalWebFluxTest(controllers = WarehouseCountController.class, controllersClasses = WarehouseCountController.class)
@ActiveProfiles("test")
public class WarehouseCountControllerTest {

        private static final Logger log = LoggerFactory.getLogger(WarehouseCountControllerTest.class);

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ObjectMapper mapper;

        @MockitoBean
        private WarehouseCountService countService;

        @BeforeEach
        void setUp() {
                // Reset mocks before each test
                Mockito.reset(countService);
        }

        @Nested
        @DisplayName("Count All Warehouses Endpoint Tests")
        class CountAllWarehousesTests {

                @Test
                @DisplayName("Should accept valid includeDeleted parameter as true")
                void shouldAcceptValidIncludeDeletedParameterTrue() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(100L)
                                        .description("Total warehouses")
                                        .build();
                        when(countService.countAllWarehouses(true)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count?includeDeleted=true")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                // Debug: Print response body
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.count").isEqualTo(100)
                                        .jsonPath("$.description").isEqualTo("Total warehouses")
                                        .jsonPath("$.links[0].rel").isEqualTo("self")
                                        .jsonPath("$.links[0].href").isEqualTo("/warehouse/count?includeDeleted=true");

                        verify(countService).countAllWarehouses(true);
                }

                @Test
                @DisplayName("Should accept valid includeDeleted parameter as false")
                void shouldAcceptValidIncludeDeletedParameterFalse() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(80L)
                                        .description("Total warehouses")
                                        .build();
                        when(countService.countAllWarehouses(false)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count?includeDeleted=false")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(80);

                        verify(countService).countAllWarehouses(false);
                }

                @Test
                @DisplayName("Should use default value false when includeDeleted is not provided")
                void shouldUseDefaultValueWhenIncludeDeletedNotProvided() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(80L)
                                        .description("Total warehouses")
                                        .build();
                        when(countService.countAllWarehouses(false)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count")
                                        .exchange()
                                        .expectStatus().isOk();

                        verify(countService).countAllWarehouses(false);
                }

                @Test
                @DisplayName("Should handle invalid boolean format for includeDeleted parameter")
                void shouldHandleInvalidBooleanFormat() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count?includeDeleted=invalid")
                                        .exchange()
                                        .expectStatus().isBadRequest();

                        verifyNoInteractions(countService);
                }
        }

        @Nested
        @DisplayName("Count History By Warehouse Endpoint Tests")
        class CountHistoryByWarehouseTests {

                @Test
                @DisplayName("Should accept valid UUID for warehouseId")
                void shouldAcceptValidUUID() {
                        // Arrange
                        UUID warehouseId = UUID.randomUUID();
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(50L)
                                        .description("History entries")
                                        .build();
                        when(countService.countHistoryByWarehouseId(warehouseId)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/" + warehouseId)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(50);

                        verify(countService).countHistoryByWarehouseId(warehouseId);
                }

                @Test
                @DisplayName("Should reject invalid UUID format for warehouseId")
                void shouldRejectInvalidUUIDFormat() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/invalid-uuid")
                                        .exchange()
                                        .expectStatus().isBadRequest();

                        verifyNoInteractions(countService);
                }

                @Test
                @DisplayName("Should reject empty warehouseId")
                void shouldRejectEmptyWarehouseId() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/")
                                        .exchange()
                                        .expectStatus().isNotFound();

                        verifyNoInteractions(countService);
                }

                @Test
                @DisplayName("Should reject null warehouseId")
                void shouldRejectNullWarehouseId() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/null")
                                        .exchange()
                                        .expectStatus().isBadRequest();

                        verifyNoInteractions(countService);
                }
        }

        @Nested
        @DisplayName("Count History By Type Endpoint Tests")
        class CountHistoryByTypeTests {

                @Test
                @DisplayName("Should accept valid type parameter - IMPORT")
                void shouldAcceptValidTypeImport() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(30L)
                                        .description("Import history entries")
                                        .build();
                        when(countService.countHistoryByType("IMPORT")).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/type/IMPORT")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(30);

                        verify(countService).countHistoryByType("IMPORT");
                }

                @Test
                @DisplayName("Should accept valid type parameter - EXPORT")
                void shouldAcceptValidTypeExport() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(20L)
                                        .description("Export history entries")
                                        .build();
                        when(countService.countHistoryByType("EXPORT")).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/type/EXPORT")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(20);

                        verify(countService).countHistoryByType("EXPORT");
                }

                @Test
                @DisplayName("Should accept lowercase type parameter")
                void shouldAcceptLowercaseType() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(15L)
                                        .description("Import history entries")
                                        .build();
                        when(countService.countHistoryByType("import")).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/type/import")
                                        .exchange()
                                        .expectStatus().isOk();

                        verify(countService).countHistoryByType("import");
                }

                @Test
                @DisplayName("Should reject empty type parameter")
                void shouldRejectEmptyType() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/type/")
                                        .exchange()
                                        .expectStatus().isNotFound();

                        verifyNoInteractions(countService);
                }
        }

        @Nested
        @DisplayName("Count History By Warehouse And Type Endpoint Tests")
        class CountHistoryByWarehouseAndTypeTests {

                @Test
                @DisplayName("Should accept valid UUID and type parameters")
                void shouldAcceptValidUUIDAndType() {
                        // Arrange
                        UUID warehouseId = UUID.randomUUID();
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(25L)
                                        .description("Warehouse import history")
                                        .build();
                        when(countService.countHistoryByWarehouseIdAndType(warehouseId, "IMPORT"))
                                        .thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/" + warehouseId + "/type/IMPORT")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(25);

                        verify(countService).countHistoryByWarehouseIdAndType(warehouseId, "IMPORT");
                }

                @Test
                @DisplayName("Should reject invalid UUID with valid type")
                void shouldRejectInvalidUUIDWithValidType() {
                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/invalid-uuid/type/IMPORT")
                                        .exchange()
                                        .expectStatus().isBadRequest();

                        verifyNoInteractions(countService);
                }

                @Test
                @DisplayName("Should accept valid UUID with any type string")
                void shouldAcceptValidUUIDWithAnyType() {
                        // Arrange
                        UUID warehouseId = UUID.randomUUID();
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(10L)
                                        .description("Warehouse history")
                                        .build();
                        when(countService.countHistoryByWarehouseIdAndType(warehouseId, "EXPORT"))
                                        .thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/history/warehouse/" + warehouseId + "/type/EXPORT")
                                        .exchange()
                                        .expectStatus().isOk();

                        verify(countService).countHistoryByWarehouseIdAndType(warehouseId, "EXPORT");
                }
        }

        @Nested
        @DisplayName("Response DTO Validation Tests")
        class ResponseDTOValidationTests {

                @Test
                @DisplayName("CountResponse should serialize correctly with kebab-case")
                void countResponseShouldSerializeWithKebabCase() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(100L)
                                        .description("Test description")
                                        .build();
                        when(countService.countAllWarehouses(false)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(100)
                                        .jsonPath("$.description").isEqualTo("Test description");
                }

                @Test
                @DisplayName("StatisticsResponse should serialize correctly with kebab-case")
                void statisticsResponseShouldSerializeWithKebabCase() {
                        // Arrange
                        WarehouseCountDTO.StatisticsResponse response = WarehouseCountDTO.StatisticsResponse.builder()
                                        .totalWarehouses(100L)
                                        .inStock(80L)
                                        .outOfStock(20L)
                                        .belowMinimum(15L)
                                        .totalHistoryEntries(500L)
                                        .build();
                        when(countService.getWarehouseStatistics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/statistics")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.total-warehouses").isEqualTo(100)
                                        .jsonPath("$.in-stock").isEqualTo(80)
                                        .jsonPath("$.out-of-stock").isEqualTo(20)
                                        .jsonPath("$.below-minimum").isEqualTo(15)
                                        .jsonPath("$.total-history-entries").isEqualTo(500);
                }

                @Test
                @DisplayName("HealthMetricsResponse should serialize correctly with kebab-case")
                void healthMetricsResponseShouldSerializeWithKebabCase() {
                        // Arrange
                        WarehouseCountDTO.HealthMetricsResponse response = WarehouseCountDTO.HealthMetricsResponse
                                        .builder()
                                        .totalWarehouses(100L)
                                        .belowMinimum(15L)
                                        .outOfStock(10L)
                                        .belowMinimumPercentage(15.0)
                                        .outOfStockPercentage(10.0)
                                        .healthyPercentage(75.0)
                                        .build();
                        when(countService.getWarehouseHealthMetrics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/health")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.total-warehouses").isEqualTo(100)
                                        .jsonPath("$.below-minimum").isEqualTo(15)
                                        .jsonPath("$.out-of-stock").isEqualTo(10)
                                        .jsonPath("$.below-minimum-percentage").isEqualTo(15.0)
                                        .jsonPath("$.out-of-stock-percentage").isEqualTo(10.0)
                                        .jsonPath("$.healthy-percentage").isEqualTo(75.0);
                }

                @Test
                @DisplayName("CountResponse should handle null description")
                void countResponseShouldHandleNullDescription() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(50L)
                                        .description(null)
                                        .build();
                        when(countService.countBelowMinimum()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/below-minimum")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.count").isEqualTo(50)
                                        .jsonPath("$.description").doesNotExist();
                }

                @Test
                @DisplayName("StatisticsResponse should handle zero values")
                void statisticsResponseShouldHandleZeroValues() {
                        // Arrange
                        WarehouseCountDTO.StatisticsResponse response = WarehouseCountDTO.StatisticsResponse.builder()
                                        .totalWarehouses(0L)
                                        .inStock(0L)
                                        .outOfStock(0L)
                                        .belowMinimum(0L)
                                        .totalHistoryEntries(0L)
                                        .build();
                        when(countService.getWarehouseStatistics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/statistics")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.total-warehouses").isEqualTo(0)
                                        .jsonPath("$.in-stock").isEqualTo(0)
                                        .jsonPath("$.out-of-stock").isEqualTo(0)
                                        .jsonPath("$.below-minimum").isEqualTo(0)
                                        .jsonPath("$.total-history-entries").isEqualTo(0);
                }

                @Test
                @DisplayName("HealthMetricsResponse should handle null percentage values")
                void healthMetricsResponseShouldHandleNullPercentages() {
                        // Arrange
                        WarehouseCountDTO.HealthMetricsResponse response = WarehouseCountDTO.HealthMetricsResponse
                                        .builder()
                                        .totalWarehouses(0L)
                                        .belowMinimum(0L)
                                        .outOfStock(0L)
                                        .belowMinimumPercentage(null)
                                        .outOfStockPercentage(null)
                                        .healthyPercentage(null)
                                        .build();
                        when(countService.getWarehouseHealthMetrics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/health")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .jsonPath("$.total-warehouses").isEqualTo(0)
                                        .jsonPath("$.below-minimum").isEqualTo(0)
                                        .jsonPath("$.out-of-stock").isEqualTo(0);
                }
        }

        @Nested
        @DisplayName("HATEOAS Links Validation Tests")
        class HATEOASLinksTests {

                @Test
                @DisplayName("Count all endpoint should include HATEOAS links")
                void countAllShouldIncludeHATEOASLinks() {
                        // Arrange
                        WarehouseCountDTO.CountResponse response = WarehouseCountDTO.CountResponse.builder()
                                        .count(100L)
                                        .description("Total warehouses")
                                        .build();
                        when(countService.countAllWarehouses(false)).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                // Debug: Print response body
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.links[0].rel").isEqualTo("self")
                                        .jsonPath("$.links[0].href").isEqualTo("/warehouse/count?includeDeleted=false");
                }

                @Test
                @DisplayName("Statistics endpoint should include HATEOAS links")
                void statisticsShouldIncludeHATEOASLinks() {
                        // Arrange
                        WarehouseCountDTO.StatisticsResponse response = WarehouseCountDTO.StatisticsResponse.builder()
                                        .totalWarehouses(100L)
                                        .inStock(80L)
                                        .outOfStock(20L)
                                        .belowMinimum(15L)
                                        .totalHistoryEntries(500L)
                                        .build();
                        when(countService.getWarehouseStatistics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/statistics")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                // Debug: Print response body
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.links[0].rel").isEqualTo("self")
                                        .jsonPath("$.links[0].href").isEqualTo("/warehouse/count/statistics");
                }

                @Test
                @DisplayName("Health endpoint should include HATEOAS links")
                void healthShouldIncludeHATEOASLinks() {
                        // Arrange
                        WarehouseCountDTO.HealthMetricsResponse response = WarehouseCountDTO.HealthMetricsResponse
                                        .builder()
                                        .totalWarehouses(100L)
                                        .belowMinimum(15L)
                                        .outOfStock(10L)
                                        .belowMinimumPercentage(15.0)
                                        .outOfStockPercentage(10.0)
                                        .healthyPercentage(75.0)
                                        .build();
                        when(countService.getWarehouseHealthMetrics()).thenReturn(Mono.just(response));

                        // Act & Assert
                        webTestClient.get()
                                        .uri("/warehouse/count/health")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                // Debug: Print response body
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.links[0].rel").isEqualTo("self")
                                        .jsonPath("$.links[0].href").isEqualTo("/warehouse/count/health");
                }
        }

        private void printPrettyLog(Logger log, EntityExchangeResult<byte[]> res) {
                try {
                        Object json = mapper.readValue(res.getResponseBody(), Object.class);
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(json));
                } catch (StreamReadException e) {
                        e.printStackTrace();
                } catch (DatabindException e) {
                        e.printStackTrace();
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        @SuppressWarnings("unused")
        private void printPrettyDTOLog(Logger log, EntityExchangeResult<Response> res) {
                try {
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(res));
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }
}
