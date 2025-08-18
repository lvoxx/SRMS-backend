package io.github.lvoxx.srms.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lvoxx.srms.customer.models.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@AutoConfigureTestDatabase(replace = Replace.NONE) // Dont load String datasource autoconfig
@ActiveProfiles("test")
@DisplayName("Customer Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Mock")
})
@Testcontainers
@DataR2dbcTest
public class CustomerRepositoryTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4-alpine")
            .withDatabaseName("test")
            .withUsername("root")
            .withPassword("Te3tP4ssW@r$")
            .withInitScript("customer_test.sql");

    @DynamicPropertySource
    static void configureR2dbc(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url",
                () -> "r2dbc:postgresql://"
                        + postgres.getHost() + ":" + postgres.getFirstMappedPort()
                        + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @BeforeAll
    static void setup() {
        postgres.start();
    }

    @AfterAll
    static void tearOut() {
        postgres.stop();
    }

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
        // Save instantly and get the ID
        customer1 = repository.save(customer1).block();

        customer2 = new Customer();
        customer2.setEmail("john.doe2.cus@email.srms.com");
        customer2.setFirstName("Jane");
        customer2.setLastName("Doe 2");
        customer2.setPhoneNumber("+888888888");
        // Save instantly and get the ID
        customer2 = repository.save(customer2).block();

        deletedCustomer = new Customer();
        deletedCustomer.setEmail("john.doe2.cus@email.srms.com");
        deletedCustomer.setFirstName("Jane");
        deletedCustomer.setLastName("Doe");
        deletedCustomer.setPhoneNumber("+777777777");
        // Save instantly and get the ID
        deletedCustomer = repository.save(deletedCustomer).block();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        customer1 = null;
        customer2 = null;
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
                    assertThat(c.isRegular()).isFalse();
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
    void testDeleteById() {
        Mono<Void> deleteOperation = repository.deleteById(customer1.getId());

        StepVerifier.create(deleteOperation)
                .verifyComplete();
    }

    @Test
    void testFindAllByIsDeleted_showDeletedTrue() {
        Flux<Customer> deletedCustomers = repository.findAllByShowDeleted(true);

        StepVerifier.create(deletedCustomers)
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(deletedCustomer.getId());
                    assertThat(c.getEmail()).isEqualTo("mike.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Deleted");
                    assertThat(c.getLastName()).isEqualTo("User");
                    assertThat(c.getDeletedAt()).isNotNull();
                })
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testFindAllByIsDeleted_showDeletedFalse() {
        Flux<Customer> activeCustomers = repository.findAllByShowDeleted(false);

        StepVerifier.create(activeCustomers)
                .assertNext(c -> {
                    assertThat(c.getEmail()).isEqualTo("john.doe1.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Jane");
                    assertThat(c.getLastName()).isEqualTo("Doe");
                    assertThat(c.getDeletedAt()).isNull();
                })
                .assertNext(c -> {
                    assertThat(c.getEmail()).isEqualTo("john.doe2.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Jane");
                    assertThat(c.getLastName()).isEqualTo("Doe");
                    assertThat(c.getDeletedAt()).isNull();
                })
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testFindDeleted() {
        Flux<Customer> deletedCustomers = repository.findDeleted();

        StepVerifier.create(deletedCustomers)
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(deletedCustomer.getId());
                    assertThat(c.getEmail()).isEqualTo("mike.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Deleted");
                    assertThat(c.getLastName()).isEqualTo("User");
                    assertThat(c.getDeletedAt()).isNotNull();
                })
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testRestoreById() {
        Mono<Integer> restoreOperation = repository.restoreById(deletedCustomer.getId());

        StepVerifier.create(restoreOperation)
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void testFindPageByIsDeleted_showDeletedTrue() {
        Pageable pageable = PageRequest.of(0, 2);
        Flux<Customer> pagedDeletedCustomers = repository.findPageByIsDeleted(pageable, true);

        StepVerifier.create(pagedDeletedCustomers)
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(deletedCustomer.getId());
                    assertThat(c.getEmail()).isEqualTo("mike.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Deleted");
                    assertThat(c.getLastName()).isEqualTo("User");
                    assertThat(c.getDeletedAt()).isNotNull();
                })
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testFindPageByIsDeleted_showDeletedFalse() {
        Pageable pageable = PageRequest.of(0, 2);
        Flux<Customer> pagedActiveCustomers = repository.findPageByIsDeleted(pageable, false);

        StepVerifier.create(pagedActiveCustomers)
                .assertNext(c -> {
                    assertThat(c.getEmail()).isEqualTo("john.doe1.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Jane");
                    assertThat(c.getLastName()).isEqualTo("Doe");
                    assertThat(c.getDeletedAt()).isNull();
                })
                .assertNext(c -> {
                    assertThat(c.getEmail()).isEqualTo("john.doe2.cus@email.srms.com");
                    assertThat(c.getFirstName()).isEqualTo("Jane");
                    assertThat(c.getLastName()).isEqualTo("Doe");
                    assertThat(c.getDeletedAt()).isNull();
                })
                .expectNextCount(0)
                .verifyComplete();
    }
}
