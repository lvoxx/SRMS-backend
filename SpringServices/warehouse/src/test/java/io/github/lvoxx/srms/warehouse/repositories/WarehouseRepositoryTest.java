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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.warehouse.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ImportAutoConfiguration(exclude = CacheAutoConfiguration.class)
@ActiveProfiles("repo")
@DisplayName("Warehouse Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
public class WarehouseRepositoryTest extends AbstractDatabaseTestContainer {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(WarehouseRepositoryTest.class);

    @Autowired
    WarehouseRepository repository;

    private Warehouse testWarehouse1;
    private Warehouse testWarehouse2;
    private Warehouse testWarehouse3;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        repository.deleteAll().block();

        // Create test data
        testWarehouse1 = Warehouse.builder()
                .productName("Test Pizza Flour")
                .quantity(100)
                .minQuantity(20)
                .isDeleted(false)
                .build();

        testWarehouse2 = Warehouse.builder()
                .productName("Test Mozzarella Cheese")
                .quantity(5)
                .minQuantity(50)
                .isDeleted(false)
                .build();

        testWarehouse3 = Warehouse.builder()
                .productName("Test Pepperoni")
                .quantity(0)
                .minQuantity(10)
                .isDeleted(false)
                .build();

        testWarehouse1 = repository.save(testWarehouse1).block();
        testWarehouse2 = repository.save(testWarehouse2).block();
        testWarehouse3 = repository.save(testWarehouse3).block();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll().block();
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find warehouse by ID when not deleted")
        void shouldFindWarehouseById_WhenNotDeleted() {
            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), false))
                    .assertNext(warehouse -> {
                        assertThat(warehouse).isNotNull();
                        assertThat(warehouse.getId()).isEqualTo(testWarehouse1.getId());
                        assertThat(warehouse.getProductName()).isEqualTo("Test Pizza Flour");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should not find deleted warehouse when includeDeleted is false")
        void shouldNotFindDeletedWarehouse_WhenIncludeDeletedIsFalse() {
            // Mark as deleted
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), false))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find deleted warehouse when includeDeleted is true")
        void shouldFindDeletedWarehouse_WhenIncludeDeletedIsTrue() {
            // Mark as deleted
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), true))
                    .assertNext(warehouse -> {
                        assertThat(warehouse).isNotNull();
                        assertThat(warehouse.getIsDeleted()).isTrue();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find By Product Name Tests")
    class FindByProductNameTests {

        @Test
        @DisplayName("Should find warehouse by exact product name")
        void shouldFindWarehouseByProductName() {
            StepVerifier.create(
                    repository.findByProductName("Test Pizza Flour", false))
                    .assertNext(warehouse -> {
                        assertThat(warehouse).isNotNull();
                        assertThat(warehouse.getProductName()).isEqualTo("Test Pizza Flour");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find warehouse by product name case insensitive")
        void shouldFindWarehouseByProductName_CaseInsensitive() {
            StepVerifier.create(
                    repository.findByProductName("test pizza flour", false))
                    .assertNext(warehouse -> {
                        assertThat(warehouse).isNotNull();
                        assertThat(warehouse.getProductName()).isEqualTo("Test Pizza Flour");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should not find non-existent product")
        void shouldNotFindNonExistentProduct() {
            StepVerifier.create(
                    repository.findByProductName("Non Existent Product", false))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Find All With Filters Tests")
    class FindAllWithFiltersTests {

        @Test
        @DisplayName("Should find all warehouses without filters")
        void shouldFindAllWarehouses() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            false, null, null, null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by product name")
        void shouldFilterByProductName() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            false, "Pizza", null, null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getProductName()).contains("Pizza");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by quantity range")
        void shouldFilterByQuantityRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            false, null, 10, 200, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getQuantity()).isBetween(10, 200);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should filter by created date range")
        void shouldFilterByCreatedDateRange() {
            PageRequest pageRequest = PageRequest.of(0, 30);
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime yesterday = now.minusDays(1);
            OffsetDateTime tomorrow = now.plusDays(1);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            false, null, null, null, yesterday, tomorrow, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should exclude deleted items when includeDeleted is false")
        void shouldExcludeDeletedItems() {
            // Delete one warehouse
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            false, null, null, null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should include deleted items when includeDeleted is true")
        void shouldIncludeDeletedItems() {
            // Delete one warehouse
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAllWithFilters(
                            true, null, null, null, null, null, null, null, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Special Query Tests")
    class SpecialQueryTests {

        @Test
        @DisplayName("Should find products below minimum quantity")
        void shouldFindProductsBelowMinimum() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findProductsBelowMinimum(pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                        assertThat(page)
                                .allMatch(w -> w.getQuantity() < w.getMinQuantity());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should find out of stock products")
        void shouldFindOutOfStockProducts() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findOutOfStock(pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                        assertThat(page.get(0).getQuantity()).isZero();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update warehouse without changing quantity")
        void shouldUpdateWarehouseWithoutQuantity() {
            UUID contactorId = UUID.randomUUID();
            String updatedBy = UUID.randomUUID().toString();
            OffsetDateTime updatedAt = OffsetDateTime.now();

            StepVerifier.create(
                    repository.updateWarehouse(
                            testWarehouse1.getId(),
                            "Updated Pizza Flour",
                            30,
                            contactorId,
                            updatedAt,
                            updatedBy))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(1);
                    })
                    .verifyComplete();

            // Verify the update
            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), false))
                    .assertNext(warehouse -> {
                        assertThat(warehouse.getProductName()).isEqualTo("Updated Pizza Flour");
                        assertThat(warehouse.getMinQuantity()).isEqualTo(30);
                        assertThat(warehouse.getQuantity()).isEqualTo(100); // Should not change
                        assertThat(warehouse.getContactorId()).isEqualTo(contactorId);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should not update deleted warehouse")
        void shouldNotUpdateDeletedWarehouse() {
            // Delete warehouse
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            StepVerifier.create(
                    repository.updateWarehouse(
                            testWarehouse1.getId(),
                            "Updated Name",
                            30,
                            null,
                            OffsetDateTime.now(),
                            null))
                    .assertNext(count -> {
                        assertThat(count).isZero();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should soft delete warehouse")
        void shouldSoftDeleteWarehouse() {
            String updatedBy = UUID.randomUUID().toString();
            OffsetDateTime updatedAt = OffsetDateTime.now();

            StepVerifier.create(
                    repository.softDelete(
                            testWarehouse1.getId(),
                            updatedAt,
                            updatedBy))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(1);
                    })
                    .verifyComplete();

            // Verify it's deleted
            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), false))
                    .verifyComplete();

            // Verify it exists when including deleted
            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), true))
                    .assertNext(warehouse -> {
                        assertThat(warehouse.getIsDeleted()).isTrue();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should restore deleted warehouse")
        void shouldRestoreDeletedWarehouse() {
            // First delete
            repository.softDelete(
                    testWarehouse1.getId(),
                    OffsetDateTime.now(),
                    null).block();

            // Then restore
            String updatedBy = UUID.randomUUID().toString();
            OffsetDateTime updatedAt = OffsetDateTime.now();

            StepVerifier.create(
                    repository.restore(
                            testWarehouse1.getId(),
                            updatedAt,
                            updatedBy))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(1);
                    })
                    .verifyComplete();

            // Verify it's restored
            StepVerifier.create(
                    repository.findById(testWarehouse1.getId(), false))
                    .assertNext(warehouse -> {
                        assertThat(warehouse.getIsDeleted()).isFalse();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count all non-deleted warehouses")
        void shouldCountAllNonDeleted() {
            StepVerifier.create(
                    repository.countAll(false))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count including deleted warehouses")
        void shouldCountIncludingDeleted() {
            // Delete one
            testWarehouse1.markAsDeleted();
            repository.save(testWarehouse1).block();

            StepVerifier.create(
                    repository.countAll(true))
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count products below minimum")
        void shouldCountBelowMinimum() {
            StepVerifier.create(
                    repository.countBelowMinimum())
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should count out of stock products")
        void shouldCountOutOfStock() {
            StepVerifier.create(
                    repository.countOutOfStock())
                    .assertNext(count -> {
                        assertThat(count).isEqualTo(1);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate results with default page size 30")
        void shouldPaginateWithDefaultSize() {
            PageRequest pageRequest = PageRequest.of(0, 30);

            StepVerifier.create(
                    repository.findAll(false, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(3);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should paginate with custom page size")
        void shouldPaginateWithCustomSize() {
            PageRequest pageRequest = PageRequest.of(0, 2);

            StepVerifier.create(
                    repository.findAll(false, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should get second page")
        void shouldGetSecondPage() {
            PageRequest pageRequest = PageRequest.of(1, 1);

            StepVerifier.create(
                    repository.findAll(false, pageRequest).collectList())
                    .assertNext(page -> {
                        assertThat(page).hasSize(1);
                    })
                    .verifyComplete();
        }
    }
}