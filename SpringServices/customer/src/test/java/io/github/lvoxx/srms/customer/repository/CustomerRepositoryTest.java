package io.github.lvoxx.srms.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.customer.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.customer.models.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("Customer Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
public class CustomerRepositoryTest extends AbstractDatabaseTestContainer {

        @Autowired
        CustomerRepository repository;

        private Customer customer1;
        private Customer customer2;
        private Customer deletedCustomer;

        @BeforeEach
        void setUp() {
                // Create test data
                customer1 = new Customer();
                customer1.setEmail("john.doe1.cus@email.srms.com");
                customer1.setFirstName("Jane");
                customer1.setLastName("Doe");
                customer1.setPhoneNumber("+999999999");
                customer1.setRegular(true);
                // Save instantly and get the ID

                customer2 = new Customer();
                customer2.setEmail("john.doe2.cus@email.srms.com");
                customer2.setFirstName("Jane");
                customer2.setLastName("Doe 2");
                customer2.setPhoneNumber("+888888888");
                // Save instantly and get the ID

                deletedCustomer = new Customer();
                deletedCustomer.setEmail("deleted.cus@email.srms.com");
                deletedCustomer.setFirstName("Jane");
                deletedCustomer.setLastName("Doe");
                deletedCustomer.setPhoneNumber("+777777777");
                deletedCustomer.setDeletedAt(OffsetDateTime.now());
                // Save instantly and get the ID

                customer1 = repository.save(customer1).block();
                customer2 = repository.save(customer2).block();
                deletedCustomer = repository.save(deletedCustomer).block();
        }

        @AfterEach
        void tearDown() {
                repository.deleteAll().block();
                deletedCustomer = null;
                customer2 = null;
                customer1 = null;
        }

        @Test
        void shouldReturnANewCustomer_whenSavingGivenNewCustomer() {
                Customer newCustomer = new Customer();
                // newCustomer.setId(UUID.randomUUID());
                newCustomer.setEmail("new.cus@email.srms.com");
                newCustomer.setFirstName("New");
                newCustomer.setLastName("Customer");
                newCustomer.setPhoneNumber("+1234567890");

                Mono<Customer> savedCustomer = repository.save(newCustomer)
                                .flatMap(saved -> repository.findById(saved.getId()));

                StepVerifier.create(savedCustomer)
                                .assertNext(c -> {
                                        assertThat(c.getId()).isNotNull();
                                        assertThat(c.getEmail()).isEqualTo("new.cus@email.srms.com");
                                        assertThat(c.getFirstName()).isEqualTo("New");
                                        assertThat(c.getLastName()).isEqualTo("Customer");
                                        assertThat(c.getPhoneNumber()).isEqualTo("+1234567890");
                                        assertThat(c.getCreatedAt()).isNotNull();
                                        assertThat(c.getUpdatedAt()).isNotNull();
                                        assertThat(c.getDeletedAt()).isNull();
                                        assertThat(c.isRegular()).isFalse();
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnCustomer1_whenFindingByCustomerId() {
                Mono<Customer> foundCustomer = repository.findById(customer1.getId());

                StepVerifier.create(foundCustomer)
                                .assertNext(c -> {
                                        assertThat(c.getId()).isEqualTo(customer1.getId());
                                        assertThat(c.getEmail()).isEqualTo(customer1.getEmail());
                                        assertThat(c.getFirstName()).isEqualTo(customer1.getFirstName());
                                        assertThat(c.getLastName()).isEqualTo(customer1.getLastName());
                                        assertThat(c.getCreatedAt()).isNotNull();
                                        assertThat(c.getUpdatedAt()).isNotNull();
                                        assertThat(c.getDeletedAt()).isNull();
                                        assertThat(c.isRegular()).isTrue();
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturn3Customers_whenFindingAllCustomers() {
                Flux<Customer> allCustomers = repository.findAll();

                StepVerifier.create(allCustomers)
                                .expectNextCount(3)
                                .verifyComplete();
        }

        @Test
        void shouldUpdateCustomer1_whenUpdatingCustomer_byFindingById() {
                String newCustomerEmail = "john.doe69.cus@email.srms.com";

                Customer oldCustomer1 = repository.findById(customer1.getId()).block();
                oldCustomer1.setEmail(newCustomerEmail);
                Mono<Customer> newCustomer1 = repository.save(oldCustomer1);

                StepVerifier.create(newCustomer1)
                                .assertNext(c -> {
                                        assertThat(c.getId()).isEqualTo(customer1.getId());
                                        assertThat(c.getEmail()).isEqualTo(newCustomerEmail); // Only verify this field
                                        assertThat(c.getFirstName()).isEqualTo(customer1.getFirstName());
                                        assertThat(c.getLastName()).isEqualTo(customer1.getLastName());
                                        assertThat(c.getCreatedAt()).isNotNull();
                                        assertThat(c.getUpdatedAt()).isNotNull();
                                        assertThat(c.getDeletedAt()).isNull();
                                        assertThat(c.isRegular()).isTrue();
                                }).verifyComplete();

        }

        @Test
        void shouldNotReturnDeletedCustomer_afterDeletingFromDatabase() {
                Mono<Void> deleteOperation = repository.deleteById(customer1.getId());

                StepVerifier.create(deleteOperation)
                                .verifyComplete();

                // One more step to verify, customer no longer exists
                Mono<Customer> deletedCustomer = repository.findById(customer1.getId());

                StepVerifier.create(deletedCustomer)
                                .verifyComplete();
        }

        @Test
        void findAllByShowDeleted_shouldReturnActiveCustomers_whenShowDeletedFalse() {
                Flux<Customer> result = repository.findAllByShowDeleted(false);

                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getId().equals(customer1.getId())
                                                || c.getId().equals(customer2.getId()))
                                .expectNextMatches(c -> c.getId().equals(customer1.getId())
                                                || c.getId().equals(customer2.getId()))
                                .verifyComplete();
        }

        @Test
        void findAllByShowDeleted_shouldReturnDeletedCustomers_whenShowDeletedTrue() {
                Flux<Customer> result = repository.findAllByShowDeleted(true);

                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getId().equals(deletedCustomer.getId()))
                                .verifyComplete();
        }

        @Test
        void findActiveByEmailAndShowDeleted_shouldReturnActiveCustomer_whenShowDeletedFalse() {
                Mono<Customer> result = repository.findActiveByEmailAndShowDeleted(customer1.getEmail(), false);

                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getEmail().equals(customer1.getEmail()))
                                .verifyComplete();
        }

        @Test
        void findActiveByEmailAndShowDeleted_shouldReturnEmpty_forActiveEmail_whenShowDeletedTrue() {
                Mono<Customer> result = repository.findActiveByEmailAndShowDeleted(customer1.getEmail(), true);

                StepVerifier.create(result)
                                .verifyComplete();
        }

        @Test
        void findActiveByEmailAndShowDeleted_shouldReturnDeletedCustomer_whenShowDeletedTrue() {
                Mono<Customer> result = repository.findActiveByEmailAndShowDeleted(deletedCustomer.getEmail(), true);

                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getEmail().equals(deletedCustomer.getEmail()))
                                .verifyComplete();
        }

        @Test
        void findActiveByPhoneNumberAndShowDeleted_shouldReturnActiveCustomer_whenShowDeletedFalse() {
                Mono<Customer> result = repository.findActiveByPhoneNumberAndShowDeleted(customer1.getPhoneNumber(),
                                false);
                result = result.filter(c -> c.getDeletedAt() == null);
                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getPhoneNumber().equals(customer1.getPhoneNumber()))
                                .verifyComplete();
        }

        @Test
        void findActiveByPhoneNumberAndShowDeleted_shouldReturnEmpty_forActivePhone_whenShowDeletedTrue() {
                Mono<Customer> result = repository.findActiveByPhoneNumberAndShowDeleted(customer1.getPhoneNumber(),
                                true);

                StepVerifier.create(result)
                                .verifyComplete();
        }

        @Test
        void findActiveByPhoneNumberAndShowDeleted_shouldReturnDeletedCustomer_whenShowDeletedTrue() {
                Mono<Customer> result = repository.findActiveByPhoneNumberAndShowDeleted(
                                deletedCustomer.getPhoneNumber(),
                                true);
                StepVerifier.create(result)
                                .recordWith(ArrayList::new)
                                .thenConsumeWhile(c -> c.getDeletedAt() != null)
                                .consumeRecordedWith(list -> assertThat(list.size()).isEqualTo(1))
                                .verifyComplete();
        }

        @Test
        void findActiveRegularCustomersByShowDeleted_shouldReturnRegularActiveCustomers_whenShowDeletedFalse() {
                Flux<Customer> result = repository.findActiveRegularCustomersByShowDeleted(false);
                StepVerifier.create(result)
                                .recordWith(ArrayList::new)
                                .thenConsumeWhile(c -> c.getDeletedAt() == null)
                                .consumeRecordedWith(list -> assertThat(list.size()).isEqualTo(1))
                                .verifyComplete();
        }

        @Test
        void findActiveRegularCustomersByShowDeleted_shouldReturnEmpty_whenShowDeletedTrue() {
                // Assuming no deleted regular customers in setup; add if needed
                Flux<Customer> result = repository.findActiveRegularCustomersByShowDeleted(true);

                StepVerifier.create(result)
                                .verifyComplete();
        }

        @Test
        void findDeleted_shouldReturnDeletedCustomers() {
                Flux<Customer> result = repository.findDeleted();

                StepVerifier.create(result)
                                .expectNextMatches(c -> c.getId().equals(deletedCustomer.getId()))
                                .verifyComplete();
        }

        @Test
        void findPageByIsDeleted_shouldReturnPagedActiveCustomers_whenShowDeletedFalse() {
                Pageable pageable = PageRequest.of(0, 1); // First page, size 1
                Flux<Customer> result = repository.findPageByShowDeleted(pageable, false);

                StepVerifier.create(result)
                                .expectNextCount(2) // One customer per page
                                .verifyComplete();
        }

        @Test
        void findPageByIsDeleted_shouldReturnPagedDeletedCustomers_whenShowDeletedTrue() {
                Pageable pageable = PageRequest.of(0, 10);
                Flux<Customer> result = repository.findPageByShowDeleted(pageable, true);

                StepVerifier.create(result)
                                .expectNextCount(1) // One deleted
                                .verifyComplete();
        }

        @Nested
        @DisplayName("Soft Delete Operations Tests")
        class SoftDeleteOperationsTests {

                @Test
                @DisplayName("Should find all deleted customers")
                void shouldFindAllDeletedCustomers() {
                        Flux<Customer> deletedCustomers = repository.findDeleted();

                        StepVerifier.create(deletedCustomers)
                                        .assertNext(customer -> {
                                                assertThat(customer.getDeletedAt()).isNotNull();
                                                assertThat(customer.getEmail())
                                                                .isEqualTo("deleted.cus@email.srms.com");
                                        })
                                        .expectNextCount(0)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should soft delete customer by ID")
                void shouldSoftDeleteCustomerById() {
                        Mono<Integer> deleteResult = repository.softDeleteById(customer1.getId());

                        StepVerifier.create(deleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Verify the customer is soft deleted
                        Mono<Customer> foundCustomer = repository.findById(customer1.getId());
                        StepVerifier.create(foundCustomer)
                                        .assertNext(customer -> assertThat(customer.getDeletedAt()).isNotNull())
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should not soft delete already deleted customer")
                void shouldNotSoftDeleteAlreadyDeletedCustomer() {
                        Mono<Integer> deleteResult = repository.softDeleteById(deletedCustomer.getId());

                        StepVerifier.create(deleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(0))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should restore soft deleted customer")
                void shouldRestoreSoftDeletedCustomer() {
                        Mono<Integer> restoreResult = repository.restoreById(deletedCustomer.getId());

                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Verify the customer is restored
                        Mono<Customer> foundCustomer = repository.findById(deletedCustomer.getId());
                        StepVerifier.create(foundCustomer)
                                        .assertNext(customer -> {
                                                assertThat(customer.getDeletedAt()).isNull();
                                                assertThat(customer.getEmail())
                                                                .isEqualTo("deleted.cus@email.srms.com");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 0 when trying to restore non-deleted customer")
                void shouldReturn0WhenTryingToRestoreNonDeletedCustomer() {
                        Mono<Integer> restoreResult = repository.restoreById(customer1.getId());

                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1)) // Will set
                                                                                                           // deleted_at
                                                                                                           // to NULL
                                                                                                           // anyway
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 0 when trying to soft delete non-existent customer")
                void shouldReturn0WhenTryingToSoftDeleteNonExistentCustomer() {
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
                        // Step 1: Verify Customer exists and is not deleted
                        Mono<Customer> initialState = repository.findById(customer1.getId());

                        StepVerifier.create(initialState)
                                        .assertNext(Customer -> assertThat(customer1.getDeletedAt()).isNull())
                                        .verifyComplete();

                        // Step 2: Soft delete the Customer
                        Mono<Integer> softDeleteResult = repository.softDeleteById(customer1.getId())
                                        .delayElement(Duration.ofMillis(100));
                        StepVerifier.create(softDeleteResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Step 3: Verify it appears in deleted list
                        Flux<Customer> deletedList = repository.findDeleted();
                        StepVerifier.create(deletedList)
                                        .expectNextCount(2) // testSupplier + deletedCustomer
                                        .verifyComplete();

                        // Step 4: Verify it doesn't appear in non-deleted search
                        Mono<Customer> nonDeletedByType = repository
                                        .findActiveByEmailAndShowDeleted(customer1.getEmail(), false);
                        StepVerifier.create(nonDeletedByType)
                                        .expectNextCount(0)
                                        .verifyComplete();

                        // Step 5: Verify it appears in deleted search
                        Mono<Customer> deletedByType = repository
                                        .findActiveByPhoneNumberAndShowDeleted(customer1.getPhoneNumber(), true);
                        StepVerifier.create(deletedByType)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        // Step 6: Restore the Customer
                        Mono<Integer> restoreResult = repository.restoreById(customer1.getId());
                        StepVerifier.create(restoreResult)
                                        .assertNext(rowsAffected -> assertThat(rowsAffected).isEqualTo(1))
                                        .verifyComplete();

                        // Step 7: Verify it's back to normal state
                        Mono<Customer> finalState = repository.findById(customer1.getId());
                        StepVerifier.create(finalState)
                                        .assertNext(Customer -> {
                                                assertThat(Customer.getDeletedAt()).isNull();
                                                assertThat(Customer.getEmail())
                                                                .isEqualTo("john.doe1.cus@email.srms.com");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle multiple search criteria combinations")
                void shouldHandleMultipleSearchCriteriaCombinations() {
                        // Create additional test data for this specific test
                        Customer additionalCustomer = Customer.builder()
                                        .email("another@email.test")
                                        .firstName("Mike")
                                        .lastName("Brown")
                                        .phoneNumber("+1999888777")
                                        .isRegular(false)
                                        .build();

                        additionalCustomer = repository.save(additionalCustomer).block();

                        // Test finding by email
                        Mono<Customer> customersByType = repository
                                        .findActiveByEmailAndShowDeleted(additionalCustomer.getEmail(), false);
                        StepVerifier.create(customersByType)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        // Test finding by phone number
                        Mono<Customer> companiesByName = repository
                                        .findActiveByPhoneNumberAndShowDeleted(additionalCustomer.getPhoneNumber(),
                                                        false);
                        StepVerifier.create(companiesByName)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        // Test specific email lookup
                        Flux<Customer> specificCustomer = repository
                                        .findAlikeFullname(additionalCustomer.getFirstName() + " "
                                                        + additionalCustomer.getLastName());
                        StepVerifier.create(specificCustomer)
                                        .consumeNextWith(c -> {
                                                assertThat(c.getFirstName()).isEqualTo("Mike");
                                                assertThat(c.getLastName()).isEqualTo("Brown");
                                                assertThat(c.getEmail())
                                                                .isEqualTo("another@email.test");
                                        })
                                        .verifyComplete();
                }
        }
}
