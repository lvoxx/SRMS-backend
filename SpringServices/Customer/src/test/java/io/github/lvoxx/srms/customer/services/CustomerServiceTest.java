package io.github.lvoxx.srms.customer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.common.exception.model.ConflictException;
import io.github.lvoxx.srms.common.exception.model.DataPersistantException;
import io.github.lvoxx.srms.common.exception.model.NotFoundException;
import io.github.lvoxx.srms.common.utils.MessageUtils;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Customer Service Tests")
@Tags({
                @Tag("Service"), @Tag("Mock"), @Tag("Message"), @Tag("Integate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({ "unused", "null" })
public class CustomerServiceTest {

        @InjectMocks
        CustomerService service;

        @Mock
        CustomerRepository repository;

        @Mock
        CustomerMapper mapper;

        @Mock
        private MessageUtils messageUtils;

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

        private CacheManager cacheManager = new ConcurrentMapCacheManager("customers");
        private Cache customerCache;

        @BeforeEach
        void setUp() {
                // Initialize cache
                customerCache = cacheManager.getCache("customers");
                customerCache.clear();

                // Setup test data
                customerId = UUID.randomUUID();

                // Setup test data
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

                setupMessageUtilsMocks();
        }

        private void setupMessageUtilsMocks() {
                // Error messages for not found scenarios
                when(messageUtils.getMessage(eq("error.resource_not_found.id"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Customer not found with id " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.resource_not_found.active_id"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Customer not found with id " + args[0] + ". Or already deleted.";
                                });

                when(messageUtils.getMessage(eq("error.resource_not_found.email"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Customer not found with email " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.resource_not_found.phone_number"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Customer not found with phone number " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.resource_not_found.deleted"), any()))
                                .thenReturn("Customer is deleted. Contact admin to restore the customer.");

                // Error messages for update operations
                when(messageUtils.getMessage(eq("error.resource_already_existed.email"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Customer with email " + args[0] + " already exists.";
                                });

                when(messageUtils.getMessage(eq("error.update.failed_to_create"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Failed to create customer with email " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.update.failed_to_update"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Failed to update customer with email " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.update.failed_to_delete"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Failed to delete customer with id " + args[0];
                                });

                when(messageUtils.getMessage(eq("error.update.failed_to_restore"), any()))
                                .thenAnswer(invocation -> {
                                        Object[] args = invocation.getArgument(1, Object[].class);
                                        return "Failed to restore customer with id " + args[0];
                                });
        }

        @Nested
        @DisplayName("Find Operations")
        class FindOperationsTest {

                @Test
                @DisplayName("Should find customer by ID successfully")
                void shouldFindCustomerByIdSuccessfully() {
                        // Given
                        when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findById(customerId))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).findById(customerId);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw NotFoundException when customer not found by ID")
                void shouldThrowNotFoundExceptionWhenCustomerNotFoundById() {
                        // Given
                        when(repository.findById(customerId)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findById(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals(
                                                                        "Customer not found with id " + customerId))
                                        .verify();

                        verify(repository).findById(customerId);
                        verify(mapper, never()).toResponse(any());
                }

                @Test
                @DisplayName("Should find customer by email successfully")
                void shouldFindCustomerByEmailSuccessfully() {
                        // Given
                        String email = "john.doe@example.com";
                        when(repository.findByEmail(email)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findByEmail(email))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).findByEmail(email);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw NotFoundException when customer not found by email")
                void shouldThrowNotFoundExceptionWhenCustomerNotFoundByEmail() {
                        // Given
                        String email = "nonexistent@example.com";
                        when(repository.findByEmail(email)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findByEmail(email))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals(
                                                                        "Customer not found with email " + email))
                                        .verify();

                        verify(repository).findByEmail(email);
                }

                @Test
                @DisplayName("Should find customer by phone number successfully")
                void shouldFindCustomerByPhoneNumberSuccessfully() {
                        // Given
                        String phoneNumber = "+1234567890";
                        when(repository.findByPhoneNumber(phoneNumber)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findByPhoneNumber(phoneNumber))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).findByPhoneNumber(phoneNumber);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw NotFoundException when customer not found by phone number")
                void shouldThrowNotFoundExceptionWhenCustomerNotFoundByPhoneNumber() {
                        // Given
                        String phoneNumber = "+9999999999";
                        when(repository.findByPhoneNumber(phoneNumber)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findByPhoneNumber(phoneNumber))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage()
                                                                        .equals("Customer not found with phone number "
                                                                                        + phoneNumber))
                                        .verify();

                        verify(repository).findByPhoneNumber(phoneNumber);
                }

                @Test
                @DisplayName("Should find customers by alike full name successfully")
                void shouldFindCustomersByAlikeFullNameSuccessfully() {
                        // Given
                        String fullName = "John Doe";
                        Customer customer2 = Customer.builder()
                                        .id(UUID.randomUUID())
                                        .firstName("Johnny")
                                        .lastName("Doe")
                                        .email("johnny.doe@example.com")
                                        .build();

                        when(repository.findAlikeFullname(fullName)).thenReturn(Flux.just(mockCustomer, customer2));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);
                        when(mapper.toResponse(customer2)).thenReturn(
                                        CustomerDTO.Response.builder()
                                                        .id(customer2.getId())
                                                        .firstName("Johnny")
                                                        .lastName("Doe")
                                                        .email("johnny.doe@example.com")
                                                        .build());

                        // When & Then
                        StepVerifier.create(service.findAlikeFullName(fullName))
                                        .expectNextCount(2)
                                        .verifyComplete();

                        verify(repository).findAlikeFullname(fullName);
                        verify(mapper, times(2)).toResponse(any(Customer.class));
                }
        }

        @Nested
        @DisplayName("Pagination Tests")
        class PaginationTest {

                @Test
                @DisplayName("Should return paged customers successfully")
                void shouldReturnPagedCustomersSuccessfully() {
                        // Given
                        PageDTO.Request pageRequest = new PageDTO.Request(0, 10, "firstName", "ASC");
                        List<Customer> customers = Arrays.asList(mockCustomer);

                        when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                                        .thenReturn(Flux.fromIterable(customers));
                        when(repository.count()).thenReturn(Mono.just(1L));
                        when(repository.findDeleted()).thenReturn(Flux.empty());
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findAllPaged(pageRequest, false))
                                        .assertNext(pageResponse -> {
                                                assertEquals(1, pageResponse.content().size());
                                                assertEquals(0, pageResponse.page());
                                                assertEquals(10, pageResponse.size());
                                                assertEquals(1L, pageResponse.totalElements());
                                                assertEquals(1, pageResponse.totalPages());
                                        })
                                        .verifyComplete();

                        verify(repository).findPageByShowDeleted(any(Pageable.class), eq(false));
                        verify(repository).count();
                }

                @Test
                @DisplayName("Should handle pagination with deleted customers")
                void shouldHandlePaginationWithDeletedCustomers() {
                        // Given
                        PageDTO.Request pageRequest = new PageDTO.Request(0, 10, "firstName", "DESC");
                        Customer deletedCustomer = Customer.builder()
                                        .id(UUID.randomUUID())
                                        .firstName("Deleted")
                                        .lastName("Customer")
                                        .email("deleted@example.com")
                                        .deletedAt(OffsetDateTime.now())
                                        .build();

                        List<Customer> allCustomers = Arrays.asList(mockCustomer, deletedCustomer);

                        when(repository.findPageByShowDeleted(any(Pageable.class), eq(true)))
                                        .thenReturn(Flux.fromIterable(allCustomers));
                        when(repository.count()).thenReturn(Mono.just(2L));
                        when(mapper.toResponse(any(Customer.class))).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findAllPaged(pageRequest, true))
                                        .assertNext(pageResponse -> {
                                                assertEquals(2, pageResponse.content().size());
                                                assertEquals(2L, pageResponse.totalElements());
                                        })
                                        .verifyComplete();

                        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                        verify(repository).findPageByShowDeleted(pageableCaptor.capture(), eq(true));

                        Pageable capturedPageable = pageableCaptor.getValue();
                        assertEquals(Sort.Direction.DESC,
                                        capturedPageable.getSort().getOrderFor("firstName").getDirection());
                }
        }

        @Nested
        @DisplayName("Create Operations")
        class CreateOperationsTest {

                @Test
                @DisplayName("Should create customer successfully")
                void shouldCreateCustomerSuccessfully() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.empty());
                        when(mapper.toCustomer(mockRequest)).thenReturn(mockCustomer);
                        when(repository.save(mockCustomer)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).findByEmail(mockRequest.getEmail());
                        verify(mapper).toCustomer(mockRequest);
                        verify(repository).save(mockCustomer);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw ConflictException when customer email already exists")
                void shouldThrowConflictExceptionWhenCustomerEmailAlreadyExists() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.just(mockCustomer));

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof ConflictException &&
                                                        throwable.getMessage().equals("Customer with email "
                                                                        + mockRequest.getEmail() + " already exists."))
                                        .verify();

                        verify(repository).findByEmail(mockRequest.getEmail());
                        verify(repository, never()).save(any());
                }

                @Test
                @DisplayName("Should throw DataPersistantException when save fails")
                void shouldThrowDataPersistantExceptionWhenSaveFails() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.empty());
                        when(mapper.toCustomer(mockRequest)).thenReturn(mockCustomer);
                        when(repository.save(mockCustomer)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to create customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();

                        verify(repository).save(mockCustomer);
                }

                @Test
                @DisplayName("Should handle repository error during create")
                void shouldHandleRepositoryErrorDuringCreate() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.empty());
                        when(mapper.toCustomer(mockRequest)).thenReturn(mockCustomer);
                        when(repository.save(mockCustomer))
                                        .thenReturn(Mono.error(new RuntimeException("Database error")));

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to create customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();
                }
        }

        @Nested
        @DisplayName("Update Operations")
        class UpdateOperationsTest {

                @Test
                @DisplayName("Should update customer successfully")
                void shouldUpdateCustomerSuccessfully() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(mockCustomer)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).findActiveById(customerId);
                        verify(mapper).updateCustomerFromRequest(mockRequest, mockCustomer);
                        verify(repository).save(mockCustomer);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw NotFoundException when updating non-existent customer")
                void shouldThrowNotFoundExceptionWhenUpdatingNonExistentCustomer() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals("Customer not found with id "
                                                                        + customerId + ". Or already deleted."))
                                        .verify();

                        verify(repository).findActiveById(customerId);
                        verify(repository, never()).save(any());
                }

                @Test
                @DisplayName("Should throw DataPersistantException when update save fails")
                void shouldThrowDataPersistantExceptionWhenUpdateSaveFails() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(mockCustomer)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to update customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();
                }
        }

        @Nested
        @DisplayName("Delete Operations")
        class DeleteOperationsTest {

                @Test
                @DisplayName("Should soft delete customer successfully")
                void shouldSoftDeleteCustomerSuccessfully() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(any(Customer.class))).thenReturn(Mono.just(mockCustomer));

                        // When & Then
                        StepVerifier.create(service.deleteCustomer(customerId))
                                        .verifyComplete();

                        verify(repository).findActiveById(customerId);

                        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
                        verify(repository).save(customerCaptor.capture());

                        Customer savedCustomer = customerCaptor.getValue();
                        assertNotNull(savedCustomer.getDeletedAt());
                }

                @Test
                @DisplayName("Should throw NotFoundException when deleting non-existent customer")
                void shouldThrowNotFoundExceptionWhenDeletingNonExistentCustomer() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.deleteCustomer(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals("Customer not found with id "
                                                                        + customerId + ". Or already deleted."))
                                        .verify();

                        verify(repository).findActiveById(customerId);
                        verify(repository, never()).save(any());
                }

                @Test
                @DisplayName("Should handle repository error during delete")
                void shouldHandleRepositoryErrorDuringDelete() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(any(Customer.class)))
                                        .thenReturn(Mono.error(new RuntimeException("Database error")));

                        // When & Then
                        StepVerifier.create(service.deleteCustomer(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to delete customer with id "
                                                                                        + customerId))
                                        .verify();
                }
        }

        @Nested
        @DisplayName("Restore Operations")
        class RestoreOperationsTest {

                @Test
                @DisplayName("Should restore customer successfully")
                void shouldRestoreCustomerSuccessfully() {
                        // Given
                        when(repository.restoreById(customerId)).thenReturn(Mono.just(1));
                        when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.restore(customerId))
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        verify(repository).restoreById(customerId);
                        verify(repository).findById(customerId);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should throw DataPersistantException when restore fails")
                void shouldThrowDataPersistantExceptionWhenRestoreFails() {
                        // Given
                        when(repository.restoreById(customerId)).thenReturn(Mono.just(0));

                        // When & Then
                        StepVerifier.create(service.restore(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to restore customer with id "
                                                                                        + customerId))
                                        .verify();

                        verify(repository).restoreById(customerId);
                        verify(repository, never()).findById(any(UUID.class));
                }

                @Test
                @DisplayName("Should throw DataPersistantException when restore returns empty")
                void shouldThrowDataPersistantExceptionWhenRestoreReturnsEmpty() {
                        // Given
                        when(repository.restoreById(customerId)).thenReturn(Mono.just(0));

                        // When & Then
                        StepVerifier.create(service.restore(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to restore customer with id "
                                                                                        + customerId))
                                        .verify();

                        verify(repository).restoreById(customerId);
                }
        }

        @Nested
        @DisplayName("Exception Message Tests")
        class ExceptionMessageTest {

                @Test
                @DisplayName("Should return correct message for customer not found by ID")
                void shouldReturnCorrectMessageForCustomerNotFoundById() {
                        // Given
                        UUID nonExistentId = UUID.randomUUID();
                        when(repository.findById(nonExistentId)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findById(nonExistentId))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals(
                                                                        "Customer not found with id " + nonExistentId))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.resource_not_found.id"),
                                        eq(new Object[] { nonExistentId }));
                }

                @Test
                @DisplayName("Should return correct message for customer not found by email")
                void shouldReturnCorrectMessageForCustomerNotFoundByEmail() {
                        // Given
                        String email = "test@example.com";
                        when(repository.findByEmail(email)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findByEmail(email))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals(
                                                                        "Customer not found with email " + email))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.resource_not_found.email"),
                                        eq(new Object[] { email }));
                }

                @Test
                @DisplayName("Should return correct message for customer not found by phone number")
                void shouldReturnCorrectMessageForCustomerNotFoundByPhoneNumber() {
                        // Given
                        String phoneNumber = "+9876543210";
                        when(repository.findByPhoneNumber(phoneNumber)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.findByPhoneNumber(phoneNumber))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage()
                                                                        .equals("Customer not found with phone number "
                                                                                        + phoneNumber))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.resource_not_found.phone_number"),
                                        eq(new Object[] { phoneNumber }));
                }

                @Test
                @DisplayName("Should return correct message for active customer not found")
                void shouldReturnCorrectMessageForActiveCustomerNotFound() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof NotFoundException &&
                                                        throwable.getMessage().equals("Customer not found with id "
                                                                        + customerId + ". Or already deleted."))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.resource_not_found.active_id"),
                                        eq(new Object[] { customerId }));
                }

                @Test
                @DisplayName("Should return correct message for email conflict")
                void shouldReturnCorrectMessageForEmailConflict() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.just(mockCustomer));

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof ConflictException &&
                                                        throwable.getMessage().equals("Customer with email "
                                                                        + mockRequest.getEmail() + " already exists."))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.resource_already_existed.email"),
                                        eq(new Object[] { mockRequest.getEmail() }));
                }

                @Test
                @DisplayName("Should return correct message for failed create")
                void shouldReturnCorrectMessageForFailedCreate() {
                        // Given
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.empty());
                        when(mapper.toCustomer(mockRequest)).thenReturn(mockCustomer);
                        when(repository.save(mockCustomer)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.create(mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to create customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.update.failed_to_create"),
                                        eq(new Object[] { mockRequest.getEmail() }));
                }

                @Test
                @DisplayName("Should return correct message for failed update")
                void shouldReturnCorrectMessageForFailedUpdate() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(mockCustomer)).thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to update customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.update.failed_to_update"),
                                        eq(new Object[] { mockRequest.getEmail() }));
                }

                @Test
                @DisplayName("Should return correct message for failed delete")
                void shouldReturnCorrectMessageForFailedDelete() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(any(Customer.class)))
                                        .thenReturn(Mono.error(new RuntimeException("Database error")));

                        // When & Then
                        StepVerifier.create(service.deleteCustomer(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to delete customer with id "
                                                                                        + customerId))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.update.failed_to_delete"),
                                        eq(new Object[] { customerId }));
                }

                @Test
                @DisplayName("Should return correct message for failed restore")
                void shouldReturnCorrectMessageForFailedRestore() {
                        // Given
                        when(repository.restoreById(customerId)).thenReturn(Mono.just(0));

                        // When & Then
                        StepVerifier.create(service.restore(customerId))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to restore customer with id "
                                                                                        + customerId))
                                        .verify();

                        verify(messageUtils).getMessage(eq("error.update.failed_to_restore"),
                                        eq(new Object[] { customerId }));
                }
        }

        @Nested
        @DisplayName("Edge Cases and Error Handling")
        class EdgeCasesTest {

                @Test
                @DisplayName("Should handle repository timeout gracefully")
                void shouldHandleRepositoryTimeoutGracefully() {
                        // Given
                        when(repository.findById(customerId))
                                        .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

                        // When & Then
                        StepVerifier.create(service.findById(customerId))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(repository).findById(customerId);
                }

                @Test
                @DisplayName("Should handle mapper exception during response conversion")
                void shouldHandleMapperExceptionDuringResponseConversion() {
                        // Given
                        when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenThrow(new RuntimeException("Mapping error"));

                        // When & Then
                        StepVerifier.create(service.findById(customerId))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(repository).findById(customerId);
                        verify(mapper).toResponse(mockCustomer);
                }

                @Test
                @DisplayName("Should handle null request gracefully on create")
                void shouldHandleNullRequestGracefullyOnCreate() {
                        // Given
                        CustomerDTO.Request nullRequest = null;

                        // When & Then - This should be handled by validation at controller level
                        // but testing service behavior
                        StepVerifier.create(service.create(nullRequest))
                                        .expectError(NullPointerException.class)
                                        .verify();
                }

                @Test
                @DisplayName("Should handle null request gracefully on update")
                void shouldHandleNullRequestGracefullyOnUpdate() {
                        // Given
                        UUID nullId = null;
                        CustomerDTO.Request nullRequest = null;

                        // When & Then - This should be handled by validation at controller level
                        // but testing service behavior
                        StepVerifier.create(service.update(nullId, nullRequest))
                                        .expectError(NullPointerException.class)
                                        .verify();
                }

                @Test
                @DisplayName("Should handle concurrent modification during update")
                void shouldHandleConcurrentModificationDuringUpdate() {
                        // Given
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(repository.save(mockCustomer))
                                        .thenReturn(Mono.error(new RuntimeException("Optimistic locking failure")));

                        // When & Then
                        StepVerifier.create(service.update(customerId, mockRequest))
                                        .expectErrorMatches(throwable -> throwable instanceof DataPersistantException &&
                                                        throwable.getMessage()
                                                                        .equals("Failed to update customer with email "
                                                                                        + mockRequest.getEmail()))
                                        .verify();
                }

                @Test
                @DisplayName("Should handle empty page request correctly")
                void shouldHandleEmptyPageRequestCorrectly() {
                        // Given
                        PageDTO.Request pageRequest = new PageDTO.Request(0, 0, "firstName", "ASC");

                        when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                                        .thenReturn(Flux.empty());
                        when(repository.count()).thenReturn(Mono.just(0L));
                        when(repository.findDeleted()).thenReturn(Flux.empty());

                        // When & Then
                        StepVerifier.create(service.findAllPaged(pageRequest, false))
                                        .assertNext(pageResponse -> {
                                                assertEquals(0, pageResponse.content().size());
                                                assertEquals(0L, pageResponse.totalElements());
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle very large page size")
                void shouldHandleVeryLargePageSize() {
                        // Given
                        PageDTO.Request pageRequest = new PageDTO.Request(0, Integer.MAX_VALUE,
                                        "firstName", "ASC");

                        when(repository.findPageByShowDeleted(any(Pageable.class), eq(false)))
                                        .thenReturn(Flux.just(mockCustomer));
                        when(repository.count()).thenReturn(Mono.just(1L));
                        when(repository.findDeleted()).thenReturn(Flux.empty());
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findAllPaged(pageRequest, false))
                                        .assertNext(pageResponse -> {
                                                assertEquals(1, pageResponse.content().size());
                                                assertEquals(100, pageRequest.size());
                                        })
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Performance and Load Tests")
        class PerformanceTest {

                @Test
                @DisplayName("Should handle multiple concurrent find operations")
                void shouldHandleMultipleConcurrentFindOperations() {
                        // Given
                        UUID id1 = UUID.randomUUID();
                        UUID id2 = UUID.randomUUID();
                        UUID id3 = UUID.randomUUID();

                        Customer customer1 = mockCustomer.toBuilder().id(id1).build();
                        Customer customer2 = mockCustomer.toBuilder().id(id2).build();
                        Customer customer3 = mockCustomer.toBuilder().id(id3).build();

                        when(repository.findById(id1)).thenReturn(Mono.just(customer1));
                        when(repository.findById(id2)).thenReturn(Mono.just(customer2));
                        when(repository.findById(id3)).thenReturn(Mono.just(customer3));

                        when(mapper.toResponse(any(Customer.class))).thenReturn(mockResponse);

                        // When
                        Flux<CustomerDTO.Response> results = Flux.merge(
                                        service.findById(id1),
                                        service.findById(id2),
                                        service.findById(id3));

                        // Then
                        StepVerifier.create(results)
                                        .expectNextCount(3)
                                        .verifyComplete();

                        verify(repository).findById(id1);
                        verify(repository).findById(id2);
                        verify(repository).findById(id3);
                }

                @Test
                @DisplayName("Should handle bulk operations efficiently")
                void shouldHandleBulkOperationsEfficiently() {
                        // Given
                        List<Customer> customers = Arrays.asList(
                                        mockCustomer,
                                        mockCustomer.toBuilder().id(UUID.randomUUID()).email("customer2@example.com")
                                                        .build(),
                                        mockCustomer.toBuilder().id(UUID.randomUUID()).email("customer3@example.com")
                                                        .build());

                        when(repository.findAlikeFullname("John Doe")).thenReturn(Flux.fromIterable(customers));
                        when(mapper.toResponse(any(Customer.class))).thenReturn(mockResponse);

                        // When & Then
                        StepVerifier.create(service.findAlikeFullName("John Doe"))
                                        .expectNextCount(3)
                                        .verifyComplete();

                        verify(repository).findAlikeFullname("John Doe");
                        verify(mapper, times(3)).toResponse(any(Customer.class));
                }
        }

        @Nested
        @DisplayName("Integration Scenarios")
        class IntegrationTest {

                @Test
                @DisplayName("Should complete full customer lifecycle")
                void shouldCompleteFullCustomerLifecycle() {
                        // Given - Create setup
                        when(repository.findByEmail(mockRequest.getEmail())).thenReturn(Mono.empty());
                        when(mapper.toCustomer(mockRequest)).thenReturn(mockCustomer);
                        when(repository.save(any(Customer.class))).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        // When - Create customer
                        StepVerifier.create(service.create(mockRequest)) // 1 save
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        // Given - Update setup
                        when(repository.findActiveById(customerId)).thenReturn(Mono.just(mockCustomer));

                        // When - Update customer
                        StepVerifier.create(service.update(customerId, mockRequest)) // 1 save
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        // When - Soft delete customer
                        StepVerifier.create(service.deleteCustomer(customerId))
                                        .verifyComplete();

                        // Given - Restore setup
                        when(repository.restoreById(customerId)).thenReturn(Mono.just(1));
                        when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));

                        // When - Restore customer
                        StepVerifier.create(service.restore(customerId)) // 1 save
                                        .expectNext(mockResponse)
                                        .verifyComplete();

                        // Then - Verify all operations were called
                        verify(repository).findByEmail(mockRequest.getEmail());
                        verify(repository, times(3)).save(any(Customer.class));
                        verify(repository, times(2)).findActiveById(customerId);
                        verify(repository).restoreById(customerId);
                        verify(repository).findById(customerId);
                }

                @Test
                @DisplayName("Should handle cascading failures properly")
                void shouldHandleCascadingFailuresProperly() {
                        // Given - First operation fails
                        when(repository.findById(customerId))
                                        .thenReturn(Mono.error(new RuntimeException("Database connection lost")));

                        // When & Then - Should propagate error without causing system failure
                        StepVerifier.create(service.findById(customerId))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        // System should still be able to handle subsequent requests
                        when(repository.findById(customerId)).thenReturn(Mono.just(mockCustomer));
                        when(mapper.toResponse(mockCustomer)).thenReturn(mockResponse);

                        StepVerifier.create(service.findById(customerId))
                                        .expectNext(mockResponse)
                                        .verifyComplete();
                }
        }
}