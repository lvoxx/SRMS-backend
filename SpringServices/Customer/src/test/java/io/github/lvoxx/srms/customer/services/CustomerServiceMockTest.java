package io.github.lvoxx.srms.customer.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Customer Service Tests With Mocking")
@Tags({
        @Tag("Service"), @Tag("Mock")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CustomerServiceMockTest {

    @InjectMocks
    CustomerService service;

    @Mock
    CustomerRepository repository;

    @Mock
    CustomerMapper mapper;

    @Captor
    ArgumentCaptor<CustomerDTO.Request> customerRequestDTOCaptor;

    @Captor
    ArgumentCaptor<UUID> idCaptor;

    @Captor
    ArgumentCaptor<Boolean> showDeletedCaptor;

    private UUID customerId;
    private Customer mockCustomer;
    private CustomerDTO.Request mockRequest;
    private CustomerDTO.Response mockResponse;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        now = OffsetDateTime.now();

        mockCustomer = Customer.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .email("john.doe@example.com")
                .dietaryRestrictions(new String[] { "Vegan", "Gluten-free" })
                .allergies(new String[] { "Nuts" })
                .isRegular(true)
                .notes("VIP customer")
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(null)
                .build();

        mockRequest = CustomerDTO.Request.builder()
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .email("john.doe@example.com")
                .dietaryRestrictions(new String[] { "Vegan", "Gluten-free" })
                .allergies(new String[] { "Nuts" })
                .isRegular(true)
                .notes("VIP customer")
                .build();

        mockResponse = CustomerDTO.Response.builder()
                .id(customerId.toString())
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .email("john.doe@example.com")
                .dietaryRestrictions(new String[] { "Vegan", "Gluten-free" })
                .allergies(new String[] { "Nuts" })
                .isRegular(true)
                .notes("VIP customer")
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(null)
                .build();

        // Setup mapper mocks
        when(mapper.toResponse(any(Customer.class))).thenReturn(mockResponse);
        when(mapper.toCustomer(any(CustomerDTO.Request.class))).thenReturn(mockCustomer);
        doNothing().when(mapper).updateCustomerFromRequest(any(CustomerDTO.Request.class), any(Customer.class));
    }

    @Nested
    @DisplayName("FindById Tests")
    public class FindByIdTests {

        @Test
        @DisplayName("Should return customer when found and not deleted")
        void shouldReturnCustomerWhenFoundAndNotDeleted() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.findById(customerId);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals(customerId.toString(), response.getId());
                        assertEquals("John", response.getFirstName());
                        assertEquals("Doe", response.getLastName());
                        assertNull(response.getDeletedAt());
                        return true;
                    })
                    .verifyComplete();

            verify(repository).findById(customerId);
        }

        @Test
        @DisplayName("Should return empty when customer is deleted")
        void shouldReturnEmptyWhenCustomerIsDeleted() {
            // Given
            Customer deletedCustomer = mockCustomer.toBuilder()
                    .deletedAt(now)
                    .build();
            when(repository.findById(customerId)).thenReturn(Mono.just(deletedCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.findById(customerId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findById(customerId);
        }

        @Test
        @DisplayName("Should return empty when customer not found")
        void shouldReturnEmptyWhenCustomerNotFound() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.empty());

            // When
            Mono<CustomerDTO.Response> result = service.findById(customerId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findById(customerId);
        }
    }

    @Nested
    @DisplayName("FindAllPaged Tests")
    public class FindAllPagedTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return paged customers successfully")
        void shouldReturnPagedCustomersSuccessfully() {
            // Given
            PageDTO.PageRequestDTO pageRequest = new PageDTO.PageRequestDTO(0, 10, "firstName", "ASC");
            List<Customer> customers = Arrays.asList(mockCustomer);

            when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                    .thenReturn(Flux.fromIterable(customers));
            when(repository.count()).thenReturn(Mono.just(1L));
            when(repository.findDeleted()).thenReturn(Flux.empty());

            // When
            Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> result = service.findAllPaged(pageRequest, false);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(page -> {
                        assertEquals(1, page.content().size());
                        assertEquals(0, page.page());
                        assertEquals(10, page.size());
                        assertEquals(1L, page.totalElements());
                        assertEquals(1, page.totalPages());
                        return true;
                    })
                    .verifyComplete();

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findPageByShowDeleted(pageableCaptor.capture(), eq(false));

            Pageable capturedPageable = pageableCaptor.getValue();
            assertEquals(0, capturedPageable.getPageNumber());
            assertEquals(10, capturedPageable.getPageSize());
            assertNotNull(capturedPageable.getSort().getOrderFor("firstName"));
            assertEquals(Sort.Direction.ASC, capturedPageable.getSort().getOrderFor("firstName").getDirection());
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should handle DESC sort direction")
        void shouldHandleDescSortDirection() {
            // Given
            PageDTO.PageRequestDTO pageRequest = new PageDTO.PageRequestDTO(0, 10, "lastName", "DESC");

            when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                    .thenReturn(Flux.empty());
            when(repository.count()).thenReturn(Mono.just(0L));
            when(repository.findDeleted()).thenReturn(Flux.empty());

            // When
            Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> result = service.findAllPaged(pageRequest, false);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(page -> {
                        assertEquals(0, page.content().size());
                        return true;
                    })
                    .verifyComplete();

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findPageByShowDeleted(pageableCaptor.capture(), eq(false));

            Pageable capturedPageable = pageableCaptor.getValue();
            assertNotNull(capturedPageable.getSort().getOrderFor("lastName"));
            assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("lastName").getDirection());
        }

        @Test
        @DisplayName("Should include deleted customers when showDeleted is true")
        void shouldIncludeDeletedCustomersWhenShowDeletedIsTrue() {
            // Given
            PageDTO.PageRequestDTO pageRequest = new PageDTO.PageRequestDTO(0, 10, "firstName", "ASC");

            when(repository.findPageByShowDeleted(any(Pageable.class), eq(true)))
                    .thenReturn(Flux.empty());
            when(repository.count()).thenReturn(Mono.just(5L));

            // When
            Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> result = service.findAllPaged(pageRequest, true);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(page -> {
                        assertEquals(5L, page.totalElements()); // Should count all when showDeleted=true
                        return true;
                    })
                    .verifyComplete();

            verify(repository).findPageByShowDeleted(any(Pageable.class), eq(true));
            verify(repository).count();
            verify(repository, never()).findDeleted(); // Should not subtract deleted count
        }
    }

    @Nested
    @DisplayName("Create Tests")
    public class CreateTests {

        @Test
        @DisplayName("Should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            // Given
            when(repository.save(any(Customer.class))).thenReturn(Mono.just(mockCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.create(mockRequest);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals("John", response.getFirstName());
                        assertEquals("Doe", response.getLastName());
                        assertEquals("+1234567890", response.getPhoneNumber());
                        assertArrayEquals(new String[] { "Vegan", "Gluten-free" }, response.getDietaryRestrictions());
                        assertArrayEquals(new String[] { "Nuts" }, response.getAllergies());
                        return true;
                    })
                    .verifyComplete();

            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(customerCaptor.capture());

            Customer savedCustomer = customerCaptor.getValue();
            assertEquals("John", savedCustomer.getFirstName());
            assertEquals("Doe", savedCustomer.getLastName());
        }

        @Test
        @DisplayName("Should handle repository save error")
        void shouldHandleRepositorySaveError() {
            // Given
            when(repository.save(any(Customer.class)))
                    .thenReturn(Mono.error(new RuntimeException("Database error")));

            // When
            Mono<CustomerDTO.Response> result = service.create(mockRequest);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "Database error".equals(throwable.getMessage()))
                    .verify();

            verify(repository).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Update Tests")
    public class UpdateTests {

        @Test
        @DisplayName("Should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            // Given
            CustomerDTO.Request updateRequest = mockRequest.toBuilder()
                    .firstName("John")
                    .lastName("Doe")
                    .dietaryRestrictions(new String[] { "Vegan", "Gluten-free" })
                    .build();

            Customer updatedCustomer = mockCustomer.toBuilder()
                    .firstName("John")
                    .lastName("Doe")
                    .dietaryRestrictions(new String[] { "Vegan", "Gluten-free" })
                    .build();

            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
            when(repository.save(any(Customer.class))).thenReturn(Mono.just(updatedCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.update(customerId, updateRequest);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals("John", response.getFirstName());
                        assertEquals("Doe", response.getLastName());
                        assertArrayEquals(new String[] { "Vegan", "Gluten-free" }, response.getDietaryRestrictions());
                        return true;
                    })
                    .verifyComplete();

            verify(repository).findById(customerId);
            verify(repository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should fail when customer not found")
        void shouldFailWhenCustomerNotFound() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.empty());

            // When
            Mono<CustomerDTO.Response> result = service.update(customerId, mockRequest);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "Customer not found or deleted".equals(throwable.getMessage()))
                    .verify();

            verify(repository).findById(customerId);
            verify(repository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should fail when customer is deleted")
        void shouldFailWhenCustomerIsDeleted() {
            // Given
            Customer deletedCustomer = mockCustomer.toBuilder()
                    .deletedAt(now)
                    .build();
            when(repository.findById(customerId)).thenReturn(Mono.just(deletedCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.update(customerId, mockRequest);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "Customer not found or deleted".equals(throwable.getMessage()))
                    .verify();

            verify(repository).findById(customerId);
            verify(repository, never()).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("SoftDelete Tests")
    public class SoftDeleteTests {

        @Test
        @DisplayName("Should soft delete customer successfully")
        void shouldSoftDeleteCustomerSuccessfully() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
            when(repository.save(any(Customer.class))).thenReturn(Mono.just(mockCustomer));

            // When
            Mono<Void> result = service.softDelete(customerId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findById(customerId);

            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(customerCaptor.capture());

            Customer deletedCustomer = customerCaptor.getValue();
            assertNotNull(deletedCustomer.getDeletedAt());
        }

        @Test
        @DisplayName("Should complete when customer not found")
        void shouldCompleteWhenCustomerNotFound() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.empty());

            // When
            Mono<Void> result = service.softDelete(customerId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findById(customerId);
            verify(repository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should complete when customer already deleted")
        void shouldCompleteWhenCustomerAlreadyDeleted() {
            // Given
            Customer deletedCustomer = mockCustomer.toBuilder()
                    .deletedAt(now)
                    .build();
            when(repository.findById(customerId)).thenReturn(Mono.just(deletedCustomer));

            // When
            Mono<Void> result = service.softDelete(customerId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findById(customerId);
            verify(repository, never()).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Restore Tests")
    public class RestoreTests {

        @Test
        @DisplayName("Should restore customer successfully")
        void shouldRestoreCustomerSuccessfully() {
            // Given
            when(repository.restoreById(customerId)).thenReturn(Mono.just(1));
            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.restore(customerId);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals(customerId.toString(), response.getId());
                        assertEquals("John", response.getFirstName());
                        return true;
                    })
                    .verifyComplete();

            verify(repository).restoreById(customerId);
            verify(repository).findById(customerId);
        }

        @Test
        @DisplayName("Should fail when no customer to restore")
        void shouldFailWhenNoCustomerToRestore() {
            // Given
            when(repository.restoreById(customerId)).thenReturn(Mono.just(0));

            // When
            Mono<CustomerDTO.Response> result = service.restore(customerId);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "No customer to restore".equals(throwable.getMessage()))
                    .verify();

            verify(repository).restoreById(customerId);
            verify(repository, never()).findById(customerId);
        }

        @Test
        @DisplayName("Should handle restore repository error")
        void shouldHandleRestoreRepositoryError() {
            // Given
            when(repository.restoreById(customerId))
                    .thenReturn(Mono.error(new RuntimeException("Database error")));

            // When
            Mono<CustomerDTO.Response> result = service.restore(customerId);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "Database error".equals(throwable.getMessage()))
                    .verify();

            verify(repository).restoreById(customerId);
            verify(repository, never()).findById(customerId);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    public class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle mapper null response gracefully")
        void shouldHandleMapperNullResponseGracefully() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
            when(mapper.toResponse(any(Customer.class))).thenReturn(null);

            // When
            Mono<CustomerDTO.Response> result = service.findById(customerId);

            // Then
            StepVerifier.create(result)
                    .expectError(NullPointerException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should handle concurrent modification during update")
        void shouldHandleConcurrentModificationDuringUpdate() {
            // Given
            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
            when(repository.save(any(Customer.class)))
                    .thenReturn(Mono.error(new RuntimeException("Optimistic locking failed")));

            // When
            Mono<CustomerDTO.Response> result = service.update(customerId, mockRequest);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                            "Optimistic locking failed".equals(throwable.getMessage()))
                    .verify();
        }

        @Test
        @DisplayName("Should handle empty page request correctly")
        void shouldHandleEmptyPageRequestCorrectly() {
            // Given
            PageDTO.PageRequestDTO pageRequest = new PageDTO.PageRequestDTO(0, 10, "firstName", "ASC");

            when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                    .thenReturn(Flux.empty());
            when(repository.count()).thenReturn(Mono.just(0L));
            when(repository.findDeleted()).thenReturn(Flux.empty());

            // When
            Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> result = service.findAllPaged(pageRequest, false);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(page -> {
                        assertEquals(0, page.content().size());
                        assertEquals(0L, page.totalElements());
                        assertEquals(0, page.totalPages());
                        return true;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle customers with null dietary restrictions and allergies")
        void shouldHandleCustomersWithNullDietaryRestrictionsAndAllergies() {
            // Given
            Customer customerWithNulls = mockCustomer.toBuilder()
                    .dietaryRestrictions(null)
                    .allergies(null)
                    .build();

            when(repository.findById(customerId)).thenReturn(Mono.just(customerWithNulls));

            // When
            Mono<CustomerDTO.Response> result = service.findById(customerId);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals("John", response.getFirstName());
                        // Should handle null arrays gracefully
                        return true;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle update with empty arrays")
        void shouldHandleUpdateWithEmptyArrays() {
            // Given
            CustomerDTO.Request updateRequest = mockRequest.toBuilder()
                    .dietaryRestrictions(new String[0])
                    .allergies(new String[0])
                    .build();

            when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
            when(repository.save(any(Customer.class))).thenReturn(Mono.just(mockCustomer));

            // When
            Mono<CustomerDTO.Response> result = service.update(customerId, updateRequest);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(response -> {
                        assertEquals(customerId.toString(), response.getId());
                        return true;
                    })
                    .verifyComplete();

            verify(repository).findById(customerId);
            verify(repository).save(any(Customer.class));
        }
    }
}