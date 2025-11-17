package io.github.lvoxx.srms.warehouse.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO.Response;
import io.github.lvoxx.srms.warehouse.dto.WarehouseSearchDTO;
import io.github.lvoxx.srms.warehouse.helper.MinimalWebFluxTest;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory.HistoryType;
import io.github.lvoxx.srms.warehouse.services.WarehouseManagementService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@DisplayName("Warehouse Management Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@MinimalWebFluxTest(controllers = WarehouseManagementController.class, controllersClasses = WarehouseManagementController.class)
@ActiveProfiles("test")
@SuppressWarnings("unchecked")
public class WarehouseManagementControllerTest {

        private static final Logger log = LoggerFactory.getLogger(WarehouseManagementControllerTest.class);
        private static final String USER_ID = "test-user-123";
        private static final UUID WAREHOUSE_ID = UUID.randomUUID();

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ObjectMapper mapper;

        @MockitoBean
        private WarehouseManagementService managementService;

        @BeforeEach
        void setUp() {
                Mockito.reset(managementService);
        }

        // ==================== CREATE WAREHOUSE TESTS ====================

        @Nested
        @DisplayName("POST /warehouse/management - Create Warehouse")
        class CreateWarehouseTests {

                @Test
                @DisplayName("Should reject when product name is blank")
                void shouldRejectBlankProductName() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when product name is blank");
                        log.info("=".repeat(80));

                        WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                        .productName("")
                                        .quantity(100)
                                        .minQuantity(10)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management")
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== IMPORT INVENTORY TESTS ====================

        @Nested
        @DisplayName("PATCH /warehouse/management/{id}/import - Import Inventory")
        class ImportInventoryTests {

                @Test
                @DisplayName("Should reject when quantity is zero")
                void shouldRejectZeroQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when import quantity is zero");
                        log.info("=".repeat(80));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/import")
                                                        .queryParam("quantity", 0)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity is negative")
                void shouldRejectNegativeQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when import quantity is negative");
                        log.info("=".repeat(80));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/import")
                                                        .queryParam("quantity", -10)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity parameter is missing")
                void shouldRejectMissingQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when quantity parameter is missing");
                        log.info("=".repeat(80));

                        webTestClient.patch()
                                        .uri("/warehouse/management/{id}/import", WAREHOUSE_ID)
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid import with quantity 1")
                void shouldAcceptValidImportMinQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid import with quantity 1");
                        log.info("=".repeat(80));

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(101)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.importInventory(eq(WAREHOUSE_ID), eq(1), eq(USER_ID)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/import")
                                                        .queryParam("quantity", 1)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid import with large quantity")
                void shouldAcceptValidImportLargeQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid import with large quantity");
                        log.info("=".repeat(80));

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(1100)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.importInventory(eq(WAREHOUSE_ID), eq(1000), eq(USER_ID)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/import")
                                                        .queryParam("quantity", 1000)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== EXPORT INVENTORY TESTS ====================

        @Nested
        @DisplayName("PATCH /warehouse/management/{id}/export - Export Inventory")
        class ExportInventoryTests {

                @Test
                @DisplayName("Should reject when quantity is zero")
                void shouldRejectZeroQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when export quantity is zero");
                        log.info("=".repeat(80));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/export")
                                                        .queryParam("quantity", 0)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity is negative")
                void shouldRejectNegativeQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when export quantity is negative");
                        log.info("=".repeat(80));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/export")
                                                        .queryParam("quantity", -10)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid export with quantity 1")
                void shouldAcceptValidExportMinQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid export with quantity 1");
                        log.info("=".repeat(80));

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(99)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.exportInventory(eq(WAREHOUSE_ID), eq(1), eq(USER_ID)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.patch()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/{id}/export")
                                                        .queryParam("quantity", 1)
                                                        .build(WAREHOUSE_ID))
                                        .header("X-User-Id", USER_ID)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== SEARCH WITH FILTERS TESTS ====================

        @Nested
        @DisplayName("GET /warehouse/management/search - Search with Filters")
        class SearchWithFiltersTests {

                @Test
                @DisplayName("Should reject when minQuantity is negative")
                void shouldRejectNegativeMinQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when minQuantity filter is negative");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/search")
                                                        .queryParam("minQuantity", -1)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when maxQuantity is negative")
                void shouldRejectNegativeMaxQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when maxQuantity filter is negative");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/search")
                                                        .queryParam("maxQuantity", -1)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when page is negative")
                void shouldRejectNegativePage() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when page is negative");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/search")
                                                        .queryParam("page", -1)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when size is zero")
                void shouldRejectZeroSize() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when size is zero");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management/search")
                                                        .queryParam("size", 0)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept search with all valid filters")
                void shouldAcceptSearchWithAllFilters() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept search with all valid filters");
                        log.info("=".repeat(80));

                        OffsetDateTime now = OffsetDateTime.now();
                        OffsetDateTime yesterday = now.minusDays(1);

                        WarehouseSearchDTO.Request request = WarehouseSearchDTO.Request.builder()
                                        .productName("Test")
                                        .minQuantity(10)
                                        .maxQuantity(100)
                                        .createdFrom(yesterday.toString())
                                        .createdTo(now.toString())
                                        .updatedFrom(yesterday.toString())
                                        .updatedTo(now.toString())
                                        .page(0)
                                        .size(20)
                                        .build();

                        when(managementService.findAllWithFilters(
                                        eq(false),
                                        eq("Test"),
                                        eq(10),
                                        eq(100),
                                        eq(yesterday),
                                        eq(now),
                                        eq(yesterday),
                                        eq(now),
                                        eq(0),
                                        eq(20))).thenReturn(Flux.empty());

                        webTestClient.post()
                                        .uri("/warehouse/management/search?includeDeleted=false")
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                printPrettyLog(log, res);
                                                log.info("\n" + "=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should not accept search with no filters")
                void shouldAcceptSearchWithNoFilters() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept search with no filters");
                        log.info("=".repeat(80));

                        // DTO rỗng (dùng default page=0, size=20)
                        WarehouseSearchDTO.Request request = WarehouseSearchDTO.Request.builder()
                                        .productName("")
                                        .minQuantity(null)
                                        .maxQuantity(null)
                                        .createdFrom(null)
                                        .createdTo(null)
                                        .updatedFrom(null)
                                        .updatedTo(null)
                                        .page(0)
                                        .size(20)
                                        .build();

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(1100)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.findAllWithFilters(
                                        anyBoolean(),
                                        any(),
                                        any(),
                                        any(),
                                        any(),
                                        any(),
                                        any(),
                                        any(),
                                        anyInt(),
                                        anyInt())).thenReturn(Flux.just(response));

                        webTestClient.post()
                                        .uri("/warehouse/management/search?includeDeleted=false")
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                printPrettyLog(log, res);
                                                log.info("\n" + "=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== FINDALL PAGINATION TESTS ====================

        @Nested
        @DisplayName("GET /warehouse/management - Find All with Pagination")
        class FindAllPaginationTests {

                @Test
                @DisplayName("Should reject when page is negative")
                void shouldRejectNegativePage() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when page is negative in findAll");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management")
                                                        .queryParam("page", -1)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when size is zero")
                void shouldRejectZeroSize() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when size is zero in findAll");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management")
                                                        .queryParam("size", 0)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when size is negative")
                void shouldRejectNegativeSize() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when size is negative in findAll");
                        log.info("=".repeat(80));

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management")
                                                        .queryParam("size", -1)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept default pagination parameters")
                void shouldAcceptDefaultPagination() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept default pagination parameters");
                        log.info("=".repeat(80));

                        when(managementService.findAll(eq(false), eq(0), eq(20)))
                                        .thenReturn(Flux.empty());

                        webTestClient.get()
                                        .uri("/warehouse/management")
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept custom valid pagination parameters")
                void shouldAcceptCustomPagination() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept custom valid pagination parameters");
                        log.info("=".repeat(80));

                        when(managementService.findAll(eq(false), eq(2), eq(50)))
                                        .thenReturn(Flux.empty());

                        webTestClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/warehouse/management")
                                                        .queryParam("page", 2)
                                                        .queryParam("size", 50)
                                                        .build())
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== BATCH OPERATIONS TESTS ====================

        @Nested
        @DisplayName("Batch Operations")
        class BatchOperationsTests {

                @Test
                @DisplayName("Should accept batch create with valid requests")
                void shouldAcceptBatchCreate() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept batch create with valid requests");
                        log.info("=".repeat(80));

                        WarehouseDTO.Request request1 = WarehouseDTO.Request.builder()
                                        .productName("Product 1")
                                        .quantity(100)
                                        .minQuantity(10)
                                        .build();

                        WarehouseDTO.Request request2 = WarehouseDTO.Request.builder()
                                        .productName("Product 2")
                                        .quantity(200)
                                        .minQuantity(20)
                                        .build();

                        WarehouseDTO.Response response1 = WarehouseDTO.Response.builder()
                                        .id(UUID.randomUUID())
                                        .productName(request1.getProductName())
                                        .quantity(request1.getQuantity())
                                        .minQuantity(request1.getMinQuantity())
                                        .createdAt(OffsetDateTime.now())
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(1L)
                                        .build();

                        WarehouseDTO.Response response2 = WarehouseDTO.Response.builder()
                                        .id(UUID.randomUUID())
                                        .productName(request2.getProductName())
                                        .quantity(request2.getQuantity())
                                        .minQuantity(request2.getMinQuantity())
                                        .createdAt(OffsetDateTime.now())
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(1L)
                                        .build();

                        when(managementService.batchCreate(any(Flux.class), eq(USER_ID)))
                                        .thenReturn(Flux.just(response1, response2));

                        webTestClient.post()
                                        .uri("/warehouse/management/batch")
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new WarehouseDTO.Request[] { request1, request2 })
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept batch delete with valid IDs")
                void shouldAcceptBatchDelete() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept batch delete with valid IDs");
                        log.info("=".repeat(80));

                        UUID id1 = UUID.randomUUID();
                        UUID id2 = UUID.randomUUID();

                        when(managementService.batchSoftDelete(any(Flux.class), eq(USER_ID)))
                                        .thenReturn(Mono.just(2L));

                        webTestClient.method(HttpMethod.DELETE) // DELETE with body = must use .method()
                                        .uri("/warehouse/management/batch")
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(List.of(id1, id2)) // JSON array: ["uuid1","uuid2"]
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody(Long.class)
                                        .value(result -> {
                                                assertEquals(2L, result);
                                        })
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                printPrettyLogWithLong(log, res);
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

        }

        @Test
        @DisplayName("Should reject when product name is null")
        void shouldRejectNullProductName() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when product name is null");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName(null)
                                .quantity(100)
                                .minQuantity(10)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when product name exceeds 255 characters")
        void shouldRejectTooLongProductName() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when product name exceeds 255 characters");
                log.info("=".repeat(80));

                String longName = "A".repeat(256);
                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName(longName)
                                .quantity(100)
                                .minQuantity(10)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when quantity is null")
        void shouldRejectNullQuantity() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when quantity is null");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(null)
                                .minQuantity(10)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when quantity is negative")
        void shouldRejectNegativeQuantity() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when quantity is negative");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(-1)
                                .minQuantity(10)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when minQuantity is null")
        void shouldRejectNullMinQuantity() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when minQuantity is null");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(100)
                                .minQuantity(null)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when minQuantity is negative")
        void shouldRejectNegativeMinQuantity() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when minQuantity is negative");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(100)
                                .minQuantity(-1)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should reject when multiple fields are invalid")
        void shouldRejectMultipleInvalidFields() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should reject when multiple fields are invalid");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("")
                                .quantity(-5)
                                .minQuantity(-10)
                                .build();

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should accept valid request with all fields")
        void shouldAcceptValidRequestWithAllFields() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should accept valid request with all fields");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(100)
                                .minQuantity(10)
                                .contactorId(UUID.randomUUID())
                                .build();

                WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                .id(WAREHOUSE_ID)
                                .productName(request.getProductName())
                                .quantity(request.getQuantity())
                                .minQuantity(request.getMinQuantity())
                                .contactorId(request.getContactorId())
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .lastUpdatedBy(USER_ID)
                                .isDeleted(false)
                                .version(1L)
                                .build();

                when(managementService.createWarehouse(any(WarehouseDTO.Request.class), eq(USER_ID)))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        @Test
        @DisplayName("Should accept valid request with zero quantity")
        void shouldAcceptValidRequestWithZeroQuantity() {
                log.info("\n" + "=".repeat(80));
                log.info("TEST: Should accept valid request with zero quantity");
                log.info("=".repeat(80));

                WarehouseDTO.Request request = WarehouseDTO.Request.builder()
                                .productName("Test Product")
                                .quantity(0)
                                .minQuantity(0)
                                .build();

                WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                .id(WAREHOUSE_ID)
                                .productName(request.getProductName())
                                .quantity(request.getQuantity())
                                .minQuantity(request.getMinQuantity())
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .lastUpdatedBy(USER_ID)
                                .isDeleted(false)
                                .version(1L)
                                .build();

                when(managementService.createWarehouse(any(WarehouseDTO.Request.class), eq(USER_ID)))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/warehouse/management")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody()
                                .consumeWith(res -> {
                                        log.info("Response Status: {}", res.getStatus());
                                        log.info("Response Body: \n{}");
                                        printPrettyLog(log, res);
                                        log.info("=".repeat(80) + "\n");
                                });
        }

        // ==================== UPDATE WAREHOUSE TESTS ====================

        @Nested
        @DisplayName("PUT /warehouse/management/{id} - Update Warehouse")
        class UpdateWarehouseTests {

                @Test
                @DisplayName("Should reject when product name exceeds 255 characters")
                void shouldRejectTooLongProductName() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when product name exceeds 255 characters in update");
                        log.info("=".repeat(80));

                        String longName = "A".repeat(256);
                        WarehouseDTO.UpdateRequest request = WarehouseDTO.UpdateRequest.builder()
                                        .productName(longName)
                                        .minQuantity(10)
                                        .build();

                        webTestClient.put()
                                        .uri("/warehouse/management/{id}", WAREHOUSE_ID)
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when minQuantity is negative")
                void shouldRejectNegativeMinQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when minQuantity is negative in update");
                        log.info("=".repeat(80));

                        WarehouseDTO.UpdateRequest request = WarehouseDTO.UpdateRequest.builder()
                                        .productName("Updated Product")
                                        .minQuantity(-1)
                                        .build();

                        webTestClient.put()
                                        .uri("/warehouse/management/{id}", WAREHOUSE_ID)
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid update with all fields")
                void shouldAcceptValidUpdateWithAllFields() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid update with all fields");
                        log.info("=".repeat(80));

                        UUID contactorId = UUID.randomUUID();
                        WarehouseDTO.UpdateRequest request = WarehouseDTO.UpdateRequest.builder()
                                        .productName("Updated Product")
                                        .minQuantity(20)
                                        .contactorId(contactorId)
                                        .build();

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName(request.getProductName())
                                        .quantity(100)
                                        .minQuantity(request.getMinQuantity())
                                        .contactorId(request.getContactorId())
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.updateWarehouse(eq(WAREHOUSE_ID), any(WarehouseDTO.UpdateRequest.class),
                                        eq(USER_ID)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.put()
                                        .uri("/warehouse/management/{id}", WAREHOUSE_ID)
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept partial update with only product name")
                void shouldAcceptPartialUpdateProductName() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept partial update with only product name");
                        log.info("=".repeat(80));

                        WarehouseDTO.UpdateRequest request = WarehouseDTO.UpdateRequest.builder()
                                        .productName("Updated Product Name Only")
                                        .build();

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName(request.getProductName())
                                        .quantity(100)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.updateWarehouse(eq(WAREHOUSE_ID), any(WarehouseDTO.UpdateRequest.class),
                                        eq(USER_ID)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.put()
                                        .uri("/warehouse/management/{id}", WAREHOUSE_ID)
                                        .header("X-User-Id", USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }
        }

        // ==================== INVENTORY TRANSACTION TESTS ====================

        @Nested
        @DisplayName("POST /warehouse/management/transaction - Process Transaction")
        class ProcessTransactionTests {

                @Test
                @DisplayName("Should reject when warehouseId is null")
                void shouldRejectNullWarehouseId() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when warehouseId is null");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(null)
                                        .quantity(10)
                                        .type(HistoryType.IMPORT)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity is null")
                void shouldRejectNullQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when quantity is null in transaction");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(null)
                                        .type(HistoryType.IMPORT)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity is zero")
                void shouldRejectZeroQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when quantity is zero in transaction");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(0)
                                        .type(HistoryType.IMPORT)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when quantity is negative")
                void shouldRejectNegativeQuantity() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when quantity is negative in transaction");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(-5)
                                        .type(HistoryType.IMPORT)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should reject when type is null")
                void shouldRejectNullType() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should reject when type is null");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(10)
                                        .type(null)
                                        .build();

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid IMPORT transaction")
                void shouldAcceptValidImportTransaction() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid IMPORT transaction");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(50)
                                        .type(HistoryType.IMPORT)
                                        .updatedBy(USER_ID)
                                        .build();

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(150)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.processInventoryTransaction(
                                        any(WarehouseDTO.InventoryTransactionRequest.class)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
                }

                @Test
                @DisplayName("Should accept valid EXPORT transaction")
                void shouldAcceptValidExportTransaction() {
                        log.info("\n" + "=".repeat(80));
                        log.info("TEST: Should accept valid EXPORT transaction");
                        log.info("=".repeat(80));

                        WarehouseDTO.InventoryTransactionRequest request = WarehouseDTO.InventoryTransactionRequest
                                        .builder()
                                        .warehouseId(WAREHOUSE_ID)
                                        .quantity(30)
                                        .type(HistoryType.EXPORT)
                                        .updatedBy(USER_ID)
                                        .build();

                        WarehouseDTO.Response response = WarehouseDTO.Response.builder()
                                        .id(WAREHOUSE_ID)
                                        .productName("Test Product")
                                        .quantity(70)
                                        .minQuantity(10)
                                        .createdAt(OffsetDateTime.now().minusDays(1))
                                        .updatedAt(OffsetDateTime.now())
                                        .lastUpdatedBy(USER_ID)
                                        .isDeleted(false)
                                        .version(2L)
                                        .build();

                        when(managementService.processInventoryTransaction(
                                        any(WarehouseDTO.InventoryTransactionRequest.class)))
                                        .thenReturn(Mono.just(response));

                        webTestClient.post()
                                        .uri("/warehouse/management/transaction")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(request)
                                        .exchange()
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                log.info("Response Status: {}", res.getStatus());
                                                log.info("Response Body: \n{}");
                                                printPrettyLog(log, res);
                                                log.info("\n");
                                                log.info("=".repeat(80) + "\n");
                                        });
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

        private void printPrettyLogWithLong(Logger log, EntityExchangeResult<Long> res) {
                Long body = res.getResponseBody();

                if (body == null) {
                        log.debug("Response: null");
                        return;
                }

                try {
                        // Convert long -> pretty JSON (vd: 2 -> "2")
                        String pretty = mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(body);

                        log.debug("Response:\n{}", pretty);
                } catch (JsonProcessingException e) {
                        log.error("Error printing response", e);
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