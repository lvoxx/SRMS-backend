package io.github.lvoxx.srms.warehouse.controllers;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.common.dto.SimpleRangeDateTimeDTO;
import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import io.github.lvoxx.srms.warehouse.helper.MinimalWebFluxTest;
import io.github.lvoxx.srms.warehouse.services.WarehouseStatisticService;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("Warehouse Statistic Controller Validation Tests")
@Tags({
        @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@MinimalWebFluxTest(controllers = WarehouseStatisticController.class, controllersClasses = WarehouseStatisticController.class)
@ActiveProfiles("test")
@SuppressWarnings("unused")
class WarehouseStatisticControllerTest {

    private static final Logger log = LoggerFactory.getLogger(WarehouseStatisticControllerTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private WarehouseStatisticService statisticService;

    private UUID testWarehouseId;
    private OffsetDateTime testFrom;
    private OffsetDateTime testTo;

    @BeforeEach
    void setUp() {
        testWarehouseId = UUID.randomUUID();
        testFrom = OffsetDateTime.now().minusDays(7);
        testTo = OffsetDateTime.now();
    }

    // ==================== IMPORT/EXPORT STATISTICS TESTS ====================

    @Test
    @DisplayName("GET /import/{warehouseId} - Should return total import quantity")
    void testGetTotalImport() {
        // Given
        WarehouseStatisticDTO.QuantityResponse mockResponse = WarehouseStatisticDTO.QuantityResponse.builder()
                .warehouseId(testWarehouseId)
                .quantity(1000L)
                .type("IMPORT")
                .description("Total import quantity")
                .build();

        when(statisticService.getTotalImportQuantity(any(UUID.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/import/{warehouseId}", testWarehouseId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.warehouse-id").isEqualTo(testWarehouseId.toString())
                .jsonPath("$.quantity").isEqualTo(1000)
                .jsonPath("$.type").isEqualTo("IMPORT")
                .jsonPath("$.links[0].rel").exists()
                .jsonPath("$.links[1].rel").isEqualTo("export")
                .jsonPath("$.links[2].rel").isEqualTo("balance");
    }

    @Test
    @DisplayName("GET /export/{warehouseId} - Should return total export quantity")
    void testGetTotalExport() {
        // Given
        WarehouseStatisticDTO.QuantityResponse mockResponse = WarehouseStatisticDTO.QuantityResponse.builder()
                .warehouseId(testWarehouseId)
                .quantity(500L)
                .type("EXPORT")
                .description("Total export quantity")
                .build();

        when(statisticService.getTotalExportQuantity(any(UUID.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/export/{warehouseId}", testWarehouseId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.warehouse-id").isEqualTo(testWarehouseId.toString())
                .jsonPath("$.quantity").isEqualTo(500)
                .jsonPath("$.type").isEqualTo("EXPORT")
                .jsonPath("$.links[0].rel").exists()
                .jsonPath("$.links[0].href").exists();
    }

    @Test
    @DisplayName("POST /quantity/{warehouseId} - Should return quantity by date range")
    void testGetQuantityByDateRange() throws Exception {
        // Given
        SimpleRangeDateTimeDTO.Request request = SimpleRangeDateTimeDTO.Request.builder()
                .from(testFrom.toString())
                .to(testTo.toString())
                .build();

        WarehouseStatisticDTO.QuantityResponse mockResponse = WarehouseStatisticDTO.QuantityResponse.builder()
                .warehouseId(testWarehouseId)
                .quantity(300L)
                .type("IMPORT")
                .fromDate(testFrom)
                .toDate(testTo)
                .description("Import quantity in date range")
                .build();

        when(statisticService.getQuantityByTypeAndDateRange(any(UUID.class), anyString(),
                any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/warehouse/statistic/quantity/{warehouseId}")
                        .queryParam("type", "IMPORT")
                        .build(testWarehouseId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.warehouse-id").isEqualTo(testWarehouseId.toString())
                .jsonPath("$.quantity").isEqualTo(300)
                .jsonPath("$.type").isEqualTo("IMPORT")
                .jsonPath("$.links[0].rel").exists();
    }

    @Test
    @DisplayName("GET /balance/{warehouseId} - Should return import/export balance")
    void testGetBalance() {
        // Given
        WarehouseStatisticDTO.BalanceResponse mockResponse = WarehouseStatisticDTO.BalanceResponse.builder()
                .warehouseId(testWarehouseId)
                .totalImport(1000L)
                .totalExport(500L)
                .balance(500L)
                .build();

        when(statisticService.getImportExportBalance(any(UUID.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/balance/{warehouseId}", testWarehouseId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.warehouse-id").isEqualTo(testWarehouseId.toString())
                .jsonPath("$.total-import").isEqualTo(1000)
                .jsonPath("$.total-export").isEqualTo(500)
                .jsonPath("$.balance").isEqualTo(500)
                .jsonPath("$.links[0].rel").exists();
    }

    // ==================== WAREHOUSE ALERTS TESTS ====================

    @Test
    @DisplayName("GET /alerts/below-minimum - Should return products below minimum")
    void testGetProductsBelowMinimum() {
        // Given
        List<WarehouseStatisticDTO.AlertItem> items = List.of(
                WarehouseStatisticDTO.AlertItem.builder()
                        .id(UUID.randomUUID())
                        .productName("Product A")
                        .currentQuantity(5)
                        .minQuantity(10)
                        .deficit(5)
                        .severity("WARNING")
                        .message("Stock below minimum threshold")
                        .updatedAt(OffsetDateTime.now())
                        .build(),
                WarehouseStatisticDTO.AlertItem.builder()
                        .id(UUID.randomUUID())
                        .productName("Product B")
                        .currentQuantity(2)
                        .minQuantity(20)
                        .deficit(18)
                        .severity("CRITICAL")
                        .message("Stock critically low")
                        .updatedAt(OffsetDateTime.now())
                        .build()
        );

        WarehouseStatisticDTO.AlertListResponse mockResponse = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(2L)
                .page(0)
                .size(20)
                .alertType("BELOW_MINIMUM")
                .build();

        when(statisticService.getProductsBelowMinimum(anyInt(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/alerts/below-minimum?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.items").isArray()
                .jsonPath("$.items.length()").isEqualTo(2)
                .jsonPath("$.total-items").isEqualTo(2)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(20)
                .jsonPath("$.alert-type").isEqualTo("BELOW_MINIMUM")
                .jsonPath("$.links[0].rel").exists();
    }

    @Test
    @DisplayName("GET /alerts/out-of-stock - Should return out of stock products")
    void testGetOutOfStockProducts() {
        // Given
        List<WarehouseStatisticDTO.AlertItem> items = List.of(
                WarehouseStatisticDTO.AlertItem.builder()
                        .id(UUID.randomUUID())
                        .productName("Product C")
                        .currentQuantity(0)
                        .minQuantity(15)
                        .deficit(15)
                        .severity("CRITICAL")
                        .message("Product out of stock")
                        .updatedAt(OffsetDateTime.now())
                        .build()
        );

        WarehouseStatisticDTO.AlertListResponse mockResponse = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(1L)
                .page(0)
                .size(20)
                .alertType("OUT_OF_STOCK")
                .build();

        when(statisticService.getOutOfStockProducts(anyInt(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/alerts/out-of-stock?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.items").isArray()
                .jsonPath("$.items[0].current-quantity").isEqualTo(0)
                .jsonPath("$.items[0].severity").isEqualTo("CRITICAL")
                .jsonPath("$.alert-type").isEqualTo("OUT_OF_STOCK")
                .jsonPath("$.links[0].rel").exists();
    }

    @Test
    @DisplayName("GET /alerts - Should return all warehouse alerts")
    void testGetAllAlerts() {
        // Given
        List<WarehouseStatisticDTO.AlertItem> items = List.of(
                WarehouseStatisticDTO.AlertItem.builder()
                        .id(UUID.randomUUID())
                        .productName("Product A")
                        .currentQuantity(5)
                        .minQuantity(10)
                        .deficit(5)
                        .severity("WARNING")
                        .message("Stock below minimum")
                        .updatedAt(OffsetDateTime.now())
                        .build(),
                WarehouseStatisticDTO.AlertItem.builder()
                        .id(UUID.randomUUID())
                        .productName("Product C")
                        .currentQuantity(0)
                        .minQuantity(15)
                        .deficit(15)
                        .severity("CRITICAL")
                        .message("Out of stock")
                        .updatedAt(OffsetDateTime.now())
                        .build()
        );

        WarehouseStatisticDTO.AlertListResponse mockResponse = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(2L)
                .page(0)
                .size(20)
                .alertType("ALL_ALERTS")
                .build();

        when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/alerts?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.items").isArray()
                .jsonPath("$.items.length()").isEqualTo(2)
                .jsonPath("$.alert-type").isEqualTo("ALL_ALERTS")
                .jsonPath("$.links[0].rel").exists();
    }

    // ==================== DASHBOARD STATISTICS TESTS ====================

    @Test
    @DisplayName("GET /dashboard - Should return dashboard statistics")
    void testGetDashboard() {
        // Given
        WarehouseStatisticDTO.DashboardResponse mockResponse = WarehouseStatisticDTO.DashboardResponse.builder()
                .totalWarehouses(100L)
                .healthyWarehouses(85L)
                .belowMinimum(10L)
                .outOfStock(5L)
                .healthPercentage(85.0)
                .totalTransactions(5000L)
                .totalImportTransactions(3000L)
                .totalExportTransactions(2000L)
                .timestamp(OffsetDateTime.now())
                .build();

        when(statisticService.getDashboardStatistics())
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/dashboard")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.total-warehouses").isEqualTo(100)
                .jsonPath("$.healthy-warehouses").isEqualTo(85)
                .jsonPath("$.below-minimum").isEqualTo(10)
                .jsonPath("$.out-of-stock").isEqualTo(5)
                .jsonPath("$.health-percentage").isEqualTo(85.0)
                .jsonPath("$.total-transactions").isEqualTo(5000)
                .jsonPath("$.links[0].rel").exists();
    }

    @Test
    @DisplayName("GET /details/{warehouseId} - Should return warehouse details")
    void testGetWarehouseDetails() {
        // Given
        WarehouseStatisticDTO.WarehouseDetailsResponse mockResponse = WarehouseStatisticDTO.WarehouseDetailsResponse.builder()
                .totalImport(1000L)
                .totalExport(500L)
                .balance(500L)
                .transactionCount(50L)
                .isBelowMinimum(false)
                .isOutOfStock(false)
                .timestamp(OffsetDateTime.now())
                .build();

        when(statisticService.getWarehouseDetails(any(UUID.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.get()
                .uri("/warehouse/statistic/details/{warehouseId}", testWarehouseId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.total-import").isEqualTo(1000)
                .jsonPath("$.total-export").isEqualTo(500)
                .jsonPath("$.balance").isEqualTo(500)
                .jsonPath("$.transaction-count").isEqualTo(50)
                .jsonPath("$.is-below-minimum").isEqualTo(false)
                .jsonPath("$.is-out-of-stock").isEqualTo(false)
                .jsonPath("$.links[0].rel").exists();
    }

    @Test
    @DisplayName("POST /time-based/{warehouseId} - Should return time-based statistics")
    void testGetTimeBasedStatistics() throws Exception {
        // Given
        SimpleRangeDateTimeDTO.Request request = SimpleRangeDateTimeDTO.Request.builder()
                .from(testFrom.toString())
                .to(testTo.toString())
                .build();

        WarehouseStatisticDTO.TimeBasedStatisticsResponse mockResponse = WarehouseStatisticDTO.TimeBasedStatisticsResponse.builder()
                .warehouseId(testWarehouseId)
                .fromDate(testFrom)
                .toDate(testTo)
                .importQuantity(800L)
                .exportQuantity(300L)
                .netChange(500L)
                .timestamp(OffsetDateTime.now())
                .build();

        when(statisticService.getTimeBasedStatistics(any(UUID.class),
                any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.post()
                .uri("/warehouse/statistic/time-based/{warehouseId}", testWarehouseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> printPrettyLog(log, response))
                .jsonPath("$.warehouse-id").isEqualTo(testWarehouseId.toString())
                .jsonPath("$.import-quantity").isEqualTo(800)
                .jsonPath("$.export-quantity").isEqualTo(300)
                .jsonPath("$.net-change").isEqualTo(500)
                .jsonPath("$.links[0].rel").exists();
    }

    // ==================== UTILITY METHODS ====================

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

    private void printPrettyLogWithLong(Logger log, EntityExchangeResult<Long> res) {
        Long body = res.getResponseBody();

        if (body == null) {
            log.debug("Response: null");
            return;
        }

        try {
            String pretty = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(body);
            log.debug("Response:\n{}", pretty);
        } catch (JsonProcessingException e) {
            log.error("Error printing response", e);
        }
    }
}