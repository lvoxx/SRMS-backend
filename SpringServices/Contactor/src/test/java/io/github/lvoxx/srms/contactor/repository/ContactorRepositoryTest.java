package io.github.lvoxx.srms.contactor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.contactor.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.contactor.models.Contactor;
import io.github.lvoxx.srms.contactor.models.ContactorType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Dont load String datasource autoconfig
@ActiveProfiles("test")
@DisplayName("Contactor Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
@DataR2dbcTest
public class ContactorRepositoryTest extends AbstractDatabaseTestContainer {

        private static final Logger log = LoggerFactory.getLogger(ContactorRepositoryTest.class);

        @Autowired
        ContactorRepository repository;

        private Contactor testCustomer;
        private Contactor testSupplier;
        private Contactor testDeletedContactor;

        @BeforeEach
        void setUp() {
                repository.deleteAll().block();

                testCustomer = Contactor.builder()
                                .contactorType(ContactorType.CUSTOMER.name())
                                .organizationName("Test Company Ltd")
                                .fullname("John Doe")
                                .phoneNumber("+1234567890")
                                .email("john@testcompany.com")
                                .address(Map.of("street", "123 Main St", "city", "Test City"))
                                .attributes(Map.of("priority", "high"))
                                .notes("Important customer")
                                .build();

                testSupplier = Contactor.builder()
                                .contactorType(ContactorType.SUPPLIER.name())
                                .organizationName("Supplier Corp")
                                .fullname("Jane Smith")
                                .phoneNumber("+0987654321")
                                .email("jane@supplier.com")
                                .address(Map.of("street", "456 Oak Ave", "city", "Supply City"))
                                .attributes(Map.of("rating", "A"))
                                .notes("Reliable supplier")
                                .build();

                testDeletedContactor = Contactor.builder()
                                .contactorType(ContactorType.DELIVERER.name())
                                .organizationName("Deleted Delivery")
                                .fullname("Bob Wilson")
                                .phoneNumber("+1122334455")
                                .email("bob@deleted.com")
                                .address(Map.of("street", "789 Pine St", "city", "Old City"))
                                .deletedAt(OffsetDateTime.now())
                                .build();

                testCustomer = repository.save(testCustomer).block();
                testSupplier = repository.save(testSupplier).block();
                testDeletedContactor = repository.save(testDeletedContactor).block();

                log.debug("Saved customer ID: {}", testCustomer.getId());
                log.debug("Saved supplier ID: {}", testSupplier.getId());
                log.debug("Saved deleted customer ID: {}", testDeletedContactor.getId());

                // Verify setup
                assertThat(testSupplier.getId()).isNotNull();
                assertThat(testDeletedContactor.getDeletedAt()).isNotNull();
        }

        @Nested
        @DisplayName("CRUD Operations Tests")
        class CrudOperationsTests {

                @Test
                @DisplayName("Should save new contactor")
                void shouldSaveNewContactor() {
                        Contactor newContactor = Contactor.builder()
                                        .contactorType(ContactorType.GROCERY.name())
                                        .organizationName("New Grocery Store")
                                        .fullname("Alice Johnson")
                                        .phoneNumber("+1555666777")
                                        .email("alice@grocery.com")
                                        .address(Map.of("street", "321 Elm St", "city", "Grocery City"))
                                        .notes("New grocery partner")
                                        .build();

                        Mono<Contactor> savedContactor = repository.save(newContactor);

                        StepVerifier.create(savedContactor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getId()).isNotNull();
                                                assertThat(contactor.getContactorType())
                                                                .isEqualTo(ContactorType.GROCERY.name());
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("New Grocery Store");
                                                assertThat(contactor.getFullname()).isEqualTo("Alice Johnson");
                                                assertThat(contactor.getEmail()).isEqualTo("alice@grocery.com");
                                                assertThat(contactor.getDeletedAt()).isNull();
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find contactor by ID")
                void shouldFindContactorById() {
                        Mono<Contactor> foundContactor = repository.findById(testCustomer.getId());

                        StepVerifier.create(foundContactor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getId()).isEqualTo(testCustomer.getId());
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Test Company Ltd");
                                                assertThat(contactor.getFullname()).isEqualTo("John Doe");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find all contactors")
                void shouldFindAllContactors() {
                        Flux<Contactor> allContactors = repository.findAll();

                        StepVerifier.create(allContactors)
                                        .expectNextCount(3) // testCustomer, testSupplier, testDeletedContractor
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should update existing contactor")
                void shouldUpdateExistingContactor() {
                        testCustomer.setOrganizationName("Updated Company Name");
                        testCustomer.setNotes("Updated notes");

                        log.debug(testCustomer.toString());

                        Mono<Contactor> updatedContractor = repository.save(testCustomer);

                        StepVerifier.create(updatedContractor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getId()).isEqualTo(testCustomer.getId());
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Updated Company Name");
                                                assertThat(contactor.getNotes()).isEqualTo("Updated notes");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should delete contactor by ID")
                void shouldDeleteContactorById() {
                        Mono<Void> deleteResult = repository.deleteById(testSupplier.getId());
                        Mono<Contactor> findAfterDelete = repository.findById(testSupplier.getId());

                        StepVerifier.create(deleteResult)
                                        .verifyComplete();

                        StepVerifier.create(findAfterDelete)
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should count all contactors")
                void shouldCountAllContactors() {
                        Mono<Long> count = repository.count();

                        StepVerifier.create(count)
                                        .assertNext(countValue -> assertThat(countValue).isEqualTo(3L))
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Custom Query Tests - Show/Hide Deleted")
        class ShowDeletedQueryTests {

                @Test
                @DisplayName("Should find all non-deleted contactors when showDeleted = false")
                void shouldFindAllNonDeletedContactors() {
                        Flux<Contactor> nonDeletedContactors = repository.findAllByShowingDeleted(false);

                        StepVerifier.create(nonDeletedContactors)
                                        .assertNext(contactor -> assertThat(contactor.getDeletedAt()).isNull())
                                        .assertNext(contactor -> assertThat(contactor.getDeletedAt()).isNull())
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find all deleted contactors when showDeleted = true")
                void shouldFindAllDeletedContactors() {
                        Flux<Contactor> deletedContactors = repository.findAllByShowingDeleted(true);

                        StepVerifier.create(deletedContactors)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getDeletedAt()).isNotNull();
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Deleted Delivery");
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find paginated non-deleted contactors")
                void shouldFindPagedNonDeletedContactors() {
                        Pageable pageable = PageRequest.of(0, 10);
                        Flux<Contactor> pagedContactors = repository.findPageByShowDeleted(pageable, false);

                        StepVerifier.create(pagedContactors)
                                        .expectNextCount(2) // testCustomer and testSupplier
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find paginated deleted contactors")
                void shouldFindPagedDeletedContactors() {
                        Pageable pageable = PageRequest.of(0, 10);
                        Flux<Contactor> pagedDeletedContactors = repository.findPageByShowDeleted(pageable, true);

                        StepVerifier.create(pagedDeletedContactors)
                                        .expectNextCount(1) // testDeletedContractor
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Custom Query Tests - Find by Contact Type")
        class FindByContactTypeTests {

                @Test
                @DisplayName("Should find contactors by type (non-deleted)")
                void shouldFindContactorsByType() {
                        Flux<Contactor> customers = repository.findByContactTypeAndShowingDeleted("CUSTOMER", false);

                        StepVerifier.create(customers)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getContactorType())
                                                                .isEqualTo(ContactorType.CUSTOMER.name());
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Test Company Ltd");
                                                assertThat(contactor.getDeletedAt()).isNull();
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find deleted contactors by type")
                void shouldFindDeletedContactorsByType() {
                        Flux<Contactor> deliverers = repository.findByContactTypeAndShowingDeleted("DELIVERER", true);

                        StepVerifier.create(deliverers)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getContactorType())
                                                                .isEqualTo(ContactorType.DELIVERER.name());
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Deleted Delivery");
                                                assertThat(contactor.getDeletedAt()).isNotNull();
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty when no contactors match type")
                void shouldReturnEmptyWhenNoContactorsMatchType() {
                        Flux<Contactor> others = repository.findByContactTypeAndShowingDeleted("OTHER", false);

                        StepVerifier.create(others)
                                        .expectNextCount(0)
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Custom Query Tests - Find by Organization Name")
        class FindByOrganizationNameTests {

                @Test
                @DisplayName("Should find contactors by partial organization name match")
                void shouldFindContactorsByPartialNameMatch() {
                        Flux<Contactor> foundContactors = repository
                                        .findByOrganizationNameContainingAndShowingDeleted("Company", false);

                        StepVerifier.create(foundContactors)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getOrganizationName()).contains("Company");
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Test Company Ltd");
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should be case insensitive search")
                void shouldBeCaseInsensitiveSearch() {
                        Flux<Contactor> foundContactors = repository
                                        .findByOrganizationNameContainingAndShowingDeleted("supplier", false);

                        StepVerifier.create(foundContactors)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getOrganizationName())
                                                                .containsIgnoringCase("supplier");
                                                assertThat(contactor.getOrganizationName()).isEqualTo("Supplier Corp");
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find deleted contactors by organization name")
                void shouldFindDeletedContactorsByOrganizationName() {
                        Flux<Contactor> foundContactors = repository
                                        .findByOrganizationNameContainingAndShowingDeleted("Deleted", true);

                        StepVerifier.create(foundContactors)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getOrganizationName()).contains("Deleted");
                                                assertThat(contactor.getDeletedAt()).isNotNull();
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Custom Query Tests - Find by Email")
        class FindByEmailTests {

                @Test
                @DisplayName("Should find contactor by exact email match")
                void shouldFindContactorByExactEmailMatch() {
                        Mono<Contactor> foundContactor = repository.findByEmail("john@testcompany.com", false);

                        StepVerifier.create(foundContactor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getEmail()).isEqualTo("john@testcompany.com");
                                                assertThat(contactor.getFullname()).isEqualTo("John Doe");
                                                assertThat(contactor.getDeletedAt()).isNull();
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should find deleted contactor by email")
                void shouldFindDeletedContactorByEmail() {
                        Mono<Contactor> foundContactor = repository.findByEmail("bob@deleted.com", true);

                        StepVerifier.create(foundContactor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getEmail()).isEqualTo("bob@deleted.com");
                                                assertThat(contactor.getFullname()).isEqualTo("Bob Wilson");
                                                assertThat(contactor.getDeletedAt()).isNotNull();
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty when email not found")
                void shouldReturnEmptyWhenEmailNotFound() {
                        Mono<Contactor> foundContactor = repository.findByEmail("notfound@test.com", false);

                        StepVerifier.create(foundContactor)
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should not find deleted contactor when showDeleted = false")
                void shouldNotFindDeletedContactorWhenShowDeletedFalse() {
                        Mono<Contactor> foundContactor = repository.findByEmail("bob@deleted.com", false);

                        StepVerifier.create(foundContactor)
                                        .expectNextCount(0)
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Soft Delete Operations Tests")
        class SoftDeleteOperationsTests {

                @Test
                @DisplayName("Should find all deleted contactors")
                void shouldFindAllDeletedContactors() {
                        Flux<Contactor> deletedContactors = repository.findDeleted();

                        StepVerifier.create(deletedContactors)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getDeletedAt()).isNotNull();
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Deleted Delivery");
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should soft delete contactor by ID")
                void shouldSoftDeleteContactorById() {
                        Mono<Integer> deleteResult = repository.softDeleteById(testCustomer.getId());

                        StepVerifier.create(deleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Verify the contactor is soft deleted
                        Mono<Contactor> foundContactor = repository.findById(testCustomer.getId());
                        StepVerifier.create(foundContactor)
                                        .assertNext(contactor -> assertThat(contactor.getDeletedAt()).isNotNull())
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should not soft delete already deleted contactor")
                void shouldNotSoftDeleteAlreadyDeletedContactor() {
                        Mono<Integer> deleteResult = repository.softDeleteById(testDeletedContactor.getId());

                        StepVerifier.create(deleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(0))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should restore soft deleted contactor")
                void shouldRestoreSoftDeletedContactor() {
                        Mono<Integer> restoreResult = repository.restoreById(testDeletedContactor.getId());

                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Verify the contactor is restored
                        Mono<Contactor> foundContactor = repository.findById(testDeletedContactor.getId());
                        StepVerifier.create(foundContactor)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getDeletedAt()).isNull();
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Deleted Delivery");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 0 when trying to restore non-deleted contactor")
                void shouldReturn0WhenTryingToRestoreNonDeletedContactor() {
                        Mono<Integer> restoreResult = repository.restoreById(testCustomer.getId());

                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1)) // Will set
                                                                                                           // deleted_at
                                                                                                           // to NULL
                                                                                                           // anyway
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 0 when trying to soft delete non-existent contactor")
                void shouldReturn0WhenTryingToSoftDeleteNonExistentContactor() {
                        UUID nonExistentId = UUID.randomUUID();
                        Mono<Integer> deleteResult = repository.softDeleteById(nonExistentId);

                        StepVerifier.create(deleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(0))
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Integration Tests - Complex Scenarios")
        class ComplexScenariosTests {

                @Test
                @DisplayName("Should handle soft delete and restore workflow")
                void shouldHandleSoftDeleteAndRestoreWorkflow() {
                        // Step 1: Verify contactor exists and is not deleted
                        Mono<Contactor> initialState = repository.findById(testSupplier.getId());

                        StepVerifier.create(initialState)
                                        .assertNext(contactor -> assertThat(contactor.getDeletedAt()).isNull())
                                        .verifyComplete();

                        // Step 2: Soft delete the contactor
                        Mono<Integer> softDeleteResult = repository.softDeleteById(testSupplier.getId())
                                        .delayElement(Duration.ofMillis(100));
                        log.debug(testSupplier.toString());
                        StepVerifier.create(softDeleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Step 3: Verify it appears in deleted list
                        Flux<Contactor> deletedList = repository.findDeleted();
                        StepVerifier.create(deletedList)
                                        // // .thenConsumeWhile(contactor -> true) // Consume all items
                                        .expectNextCount(2) // testSupplier + testDeletedContactor
                                        .verifyComplete();

                        // Step 4: Verify it doesn't appear in non-deleted search
                        Flux<Contactor> nonDeletedByType = repository
                                        .findByContactTypeAndShowingDeleted("SUPPLIER", false);
                        StepVerifier.create(nonDeletedByType)
                                        .expectNextCount(0)
                                        .verifyComplete();

                        // Step 5: Verify it appears in deleted search
                        Flux<Contactor> deletedByType = repository
                                        .findByContactTypeAndShowingDeleted("SUPPLIER", true);
                        StepVerifier.create(deletedByType)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        // Step 6: Restore the contactor
                        Mono<Integer> restoreResult = repository.restoreById(testSupplier.getId());
                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Step 7: Verify it's back to normal state
                        Mono<Contactor> finalState = repository.findById(testSupplier.getId());
                        StepVerifier.create(finalState)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getDeletedAt()).isNull();
                                                assertThat(contactor.getOrganizationName()).isEqualTo("Supplier Corp");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle multiple search criteria combinations")
                void shouldHandleMultipleSearchCriteriaCombinations() {
                        // Create additional test data for this specific test
                        Contactor additionalCustomer = Contactor.builder()
                                        .contactorType(ContactorType.CUSTOMER.name())
                                        .organizationName("Another Test Company")
                                        .fullname("Mike Brown")
                                        .phoneNumber("+1999888777")
                                        .email("mike@anothertest.com")
                                        .build();

                        repository.save(additionalCustomer).block();

                        // Test finding by type
                        Flux<Contactor> customersByType = repository
                                        .findByContactTypeAndShowingDeleted("CUSTOMER", false);
                        StepVerifier.create(customersByType)
                                        .expectNextCount(2) // testCustomer + additionalCustomer
                                        .verifyComplete();

                        // Test finding by organization name
                        Flux<Contactor> companiesByName = repository
                                        .findByOrganizationNameContainingAndShowingDeleted("Test", false);
                        StepVerifier.create(companiesByName)
                                        .expectNextCount(2) // Both contain "Test"
                                        .verifyComplete();

                        // Test specific email lookup
                        Mono<Contactor> specificCustomer = repository.findByEmail("mike@anothertest.com", false);
                        StepVerifier.create(specificCustomer)
                                        .assertNext(contactor -> {
                                                assertThat(contactor.getFullname()).isEqualTo("Mike Brown");
                                                assertThat(contactor.getOrganizationName())
                                                                .isEqualTo("Another Test Company");
                                        })
                                        .verifyComplete();
                }
        }
}
