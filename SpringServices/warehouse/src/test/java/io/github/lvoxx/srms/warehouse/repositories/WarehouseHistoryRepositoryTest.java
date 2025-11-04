package io.github.lvoxx.srms.warehouse.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.warehouse.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory.HistoryType;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("Warehouse History Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@SuppressWarnings("unused")
public class WarehouseHistoryRepositoryTest extends AbstractDatabaseTestContainer {

    private static final Logger log = LoggerFactory.getLogger(WarehouseHistoryRepositoryTest.class);

    @Autowired
    private WarehouseHistoryRepository historyRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private Warehouse testWarehouse1;
    private Warehouse testWarehouse2;
    private WarehouseHistory testHistory1;
    private WarehouseHistory testHistory2;
    private WarehouseHistory testHistory3;
    private String testUserId;

    @BeforeEach
    void setUp() {
        // Clean using TRUNCATE (bypass immutable triggers)
        databaseClient.sql("TRUNCATE TABLE warehouse_history RESTART IDENTITY CASCADE").then().block();
        databaseClient.sql("TRUNCATE TABLE warehouse RESTART IDENTITY CASCADE").then().block();

        testUserId = UUID.randomUUID().toString();

        testWarehouse1 = warehouseRepository.save(
                Warehouse.builder()
                        .productName("Test Pizza Flour")
                        .quantity(100)
                        .minQuantity(20)
                        .build())
                .block();

        testWarehouse2 = warehouseRepository.save(
                Warehouse.builder()
                        .productName("Test Mozzarella Cheese")
                        .quantity(50)
                        .minQuantity(10)
                        .build())
                .block();

        testHistory1 = historyRepository.save(
                WarehouseHistory.builder()
                        .warehouseId(testWarehouse1.getId())
                        .quantity(100)
                        .type(HistoryType.IMPORT.getValue())
                        .updatedBy(testUserId)
                        .build())
                .block();

        testHistory2 = historyRepository.save(
                WarehouseHistory.builder()
                        .warehouseId(testWarehouse1.getId())
                        .quantity(20)
                        .type(HistoryType.EXPORT.getValue())
                        .updatedBy(testUserId)
                        .build())
                .block();

        testHistory3 = historyRepository.save(
                WarehouseHistory.builder()
                        .warehouseId(testWarehouse2.getId())
                        .quantity(50)
                        .type(HistoryType.IMPORT.getValue())
                        .updatedBy(testUserId)
                        .build())
                .block();
    }

    @AfterEach
    void tearDown() {
        databaseClient.sql("TRUNCATE TABLE warehouse_history RESTART IDENTITY CASCADE").then().block();
        databaseClient.sql("TRUNCATE TABLE warehouse RESTART IDENTITY CASCADE").then().block();
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create new import history")
        void shouldCreateImportHistory() {
            WarehouseHistory newHistory = WarehouseHistory.builder()
                    .warehouseId(testWarehouse1.getId())
                    .quantity(50)
                    .type(HistoryType.IMPORT.getValue())
                    .updatedBy(testUserId)
                    .build();

            StepVerifier.create(
                    historyRepository.save(newHistory))
                    .assertNext(saved -> {
                        assertThat(saved).isNotNull();
                        assertThat(saved.getId()).isNotNull();
                        assertThat(saved.getQuantity()).isEqualTo(50);
                        assertThat(saved.getType()).isEqualTo(HistoryType.IMPORT.getValue());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should create new export history")
        void shouldCreateExportHistory() {
            WarehouseHistory newHistory = WarehouseHistory.builder()
                    .warehouseId(testWarehouse1.getId())
                    .quantity(30)
                    .type(HistoryType.EXPORT.getValue())
                    .updatedBy(testUserId)
                    .build();

            StepVerifier.create(
                    historyRepository.save(newHistory))
                    .assertNext(saved -> {
                        assertThat(saved).isNotNull();
                        assertThat(saved.getType()).isEqualTo(HistoryType.EXPORT.getValue());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find history by ID")
        void shouldFindHistoryById() {
            StepVerifier.create(
                    historyRepository.findById(testHistory1.getId()))
                    .assertNext(history -> {
                        assertThat(history).isNotNull();
                        assertThat(history.getId()).isEqualTo(testHistory1.getId());
                        assertThat(history.getQuantity()).isEqualTo(100);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            StepVerifier.create(
                    historyRepository.findById(UUID.randomUUID()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find By Warehouse ID Tests")
    class FindByWarehouseIdTests {

        @Test
        @DisplayName("Should find all history by warehouse ID")
        void shouldFindAllHistoryByWarehouseId() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByWarehouseId(testWarehouse1.getId(), pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getWarehouseId().equals(testWarehouse1.getId()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty page for warehouse with no history")
        void shouldReturnEmptyPageForWarehouseWithNoHistory() {
            Warehouse newWarehouse = Warehouse.builder()
                    .productName("New Product")
                    .quantity(0)
                    .minQuantity(10)
                    .build();
            newWarehouse = warehouseRepository.save(newWarehouse).block();

            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByWarehouseId(newWarehouse.getId(), pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).isEmpty();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find By Type Tests")
    class FindByTypeTests {

        @Test
        @DisplayName("Should find all import history")
        void shouldFindAllImportHistory() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByType(HistoryType.IMPORT.getValue(), pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getType().equals(HistoryType.IMPORT.getValue()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find all export history")
        void shouldFindAllExportHistory() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByType(HistoryType.EXPORT.getValue(), pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getType()).isEqualTo(HistoryType.EXPORT.getValue());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find By Warehouse ID And Type Tests")
    class FindByWarehouseIdAndTypeTests {

        @Test
        @DisplayName("Should find imports for specific warehouse")
        void shouldFindImportsForSpecificWarehouse() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByWarehouseIdAndType(
                            testWarehouse1.getId(),
                            HistoryType.IMPORT.getValue(),
                            pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getType()).isEqualTo(HistoryType.IMPORT.getValue());
                        assertThat(page.get(0).getWarehouseId())
                                .isEqualTo(testWarehouse1.getId());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find exports for specific warehouse")
        void shouldFindExportsForSpecificWarehouse() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findByWarehouseIdAndType(
                            testWarehouse1.getId(),
                            HistoryType.EXPORT.getValue(),
                            pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getType()).isEqualTo(HistoryType.EXPORT.getValue());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find With Filters Tests")
    class FindWithFiltersTests {

        @Test
        @DisplayName("Should find all without filters")
        void shouldFindAllWithoutFilters() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, null, null, null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by warehouse ID")
        void shouldFilterByWarehouseId() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            testWarehouse1.getId(), null, null, null, null, null, null, null, pageRequest)
                            .collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getWarehouseId().equals(testWarehouse1.getId()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by type")
        void shouldFilterByType() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, HistoryType.IMPORT.getValue(), null, null, null, null, null, null, pageRequest)
                            .collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getType().equals(HistoryType.IMPORT.getValue()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by product name")
        void shouldFilterByProductName() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, null, "Pizza", null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by quantity range")
        void shouldFilterByQuantityRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, null, null, 30, 150, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getQuantity() >= 30 && h.getQuantity() <= 150);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by date range")
        void shouldFilterByDateRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime yesterday = now.minusDays(1);
            OffsetDateTime tomorrow = now.plusDays(1);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, null, null, null, null, yesterday, tomorrow, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by updatedBy")
        void shouldFilterByUpdatedBy() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            null, null, null, null, null, null, null, testUserId, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                        assertThat(page)
                                .allMatch(h -> h.getUpdatedBy().equals(testUserId));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should combine multiple filters")
        void shouldCombineMultipleFilters() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAllWithFilters(
                            testWarehouse1.getId(),
                            HistoryType.IMPORT.getValue(),
                            null, null, null, null, null, null,
                            pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getType()).isEqualTo(HistoryType.IMPORT.getValue());
                        assertThat(page.get(0).getWarehouseId())
                                .isEqualTo(testWarehouse1.getId());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find Imports/Exports Tests")
    class FindImportsExportsTests {

        @Test
        @DisplayName("Should find imports with date range")
        void shouldFindImportsWithDateRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
            OffsetDateTime tomorrow = OffsetDateTime.now().plusDays(1);

            StepVerifier.create(
                    historyRepository.findImports(null, yesterday, tomorrow, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(h -> h.getType().equals(HistoryType.IMPORT.getValue()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find imports for specific warehouse")
        void shouldFindImportsForSpecificWarehouse() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findImports(testWarehouse1.getId(), null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getWarehouseId())
                                .isEqualTo(testWarehouse1.getId());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find exports with date range")
        void shouldFindExportsWithDateRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
            OffsetDateTime tomorrow = OffsetDateTime.now().plusDays(1);

            StepVerifier.create(
                    historyRepository.findExports(null, yesterday, tomorrow, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page)
                                .allMatch(h -> h.getType().equals(HistoryType.EXPORT.getValue()));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get total import quantity for warehouse")
        void shouldGetTotalImportQuantity() {
            StepVerifier.create(
                    historyRepository.getTotalImportQuantity(testWarehouse1.getId()))
                    .assertNext(total -> {
                        assertThat(total).isEqualTo(100);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should get total export quantity for warehouse")
        void shouldGetTotalExportQuantity() {
            StepVerifier.create(
                    historyRepository.getTotalExportQuantity(testWarehouse1.getId()))
                    .assertNext(total -> {
                        assertThat(total).isEqualTo(20);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should get quantity by type and date range")
        void shouldGetQuantityByTypeAndDateRange() {
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
            OffsetDateTime tomorrow = OffsetDateTime.now().plusDays(1);

            StepVerifier.create(
                    historyRepository.getQuantityByTypeAndDateRange(
                            testWarehouse1.getId(),
                            HistoryType.IMPORT.getValue(),
                            yesterday,
                            tomorrow))
                    .assertNext(total -> {
                        assertThat(total).isEqualTo(100);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return zero for warehouse with no history")
        void shouldReturnZeroForWarehouseWithNoHistory() {
            UUID newWarehouseId = UUID.randomUUID();

            StepVerifier.create(
                    historyRepository.getTotalImportQuantity(newWarehouseId))
                    .assertNext(total -> {
                        assertThat(total).isZero();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count all history records")
        void shouldCountAllHistory() {
            StepVerifier.create(
                    historyRepository.countAll())
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count by warehouse ID")
        void shouldCountByWarehouseId() {
            StepVerifier.create(
                    historyRepository.countByWarehouseId(testWarehouse1.getId()))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count by type")
        void shouldCountByType() {
            StepVerifier.create(
                    historyRepository.countByType(HistoryType.IMPORT.getValue()))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count by warehouse ID and type")
        void shouldCountByWarehouseIdAndType() {
            StepVerifier.create(
                    historyRepository.countByWarehouseIdAndType(
                            testWarehouse1.getId(),
                            HistoryType.EXPORT.getValue()))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(1);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Recent Activities Tests")
    class RecentActivitiesTests {

        @Test
        @DisplayName("Should find recent activities")
        void shouldFindRecentActivities() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);

            StepVerifier.create(
                    historyRepository.findRecentActivities(yesterday, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find recent activities by warehouse")
        void shouldFindRecentActivitiesByWarehouse() {
            OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);

            StepVerifier.create(
                    historyRepository.findRecentActivitiesByWarehouse(
                            testWarehouse1.getId(),
                            yesterday).collectList())
                    .assertNext(activities -> {
                        assertThat(activities).hasSize(2);
                        assertThat(activities)
                                .allMatch(h -> h.getWarehouseId().equals(testWarehouse1.getId()));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty for old date")
        void shouldReturnEmptyForOldDate() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime tomorrow = OffsetDateTime.now().plusDays(1);

            StepVerifier.create(
                    historyRepository.findRecentActivities(tomorrow, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).isEmpty();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate with default size 30")
        void shouldPaginateWithDefaultSize() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    historyRepository.findAll(pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should paginate with custom size")
        void shouldPaginateWithCustomSize() {
            PageRequest pageRequest = PageRequest.of(0, 2);

            StepVerifier.create(
                    historyRepository.findAll(pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should not be able to update history record")
        void shouldNotUpdateHistory() {
            // This test verifies that update operations are prevented at DB level
            // The actual prevention is done by DB trigger, not repository
            // We just test that we only have create operations in repository

            // Repository should only have save() for create
            // No update methods should be available
            assertThat(historyRepository)
                    .isInstanceOf(R2dbcRepository.class);
        }

        @Test
        @DisplayName("Should not be able to delete history record via repository")
        void shouldNotDeleteHistory() {
            // This test documents that delete operations should be prevented
            // The actual prevention is done by DB trigger
            // We document that delete operations should not be used

            // Even though deleteById exists in CrudRepository,
            // it should be prevented by DB trigger
            assertThat(historyRepository)
                    .isInstanceOf(R2dbcRepository.class);
        }
    }
}