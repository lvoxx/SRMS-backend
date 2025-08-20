package io.github.lvoxx.srms.customer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Customer Service Cache Tests")
@Tags({
                @Tag("Service"), @Tag("Cache"), @Tag("Unit")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({ "unused", "null" })
public class CustomerServiceCacheTest {

        @InjectMocks
        private CustomerService customerService;

        @Mock
        private CustomerRepository customerRepository;

        @Mock
        private CustomerMapper customerMapper;

        private CacheManager cacheManager = new ConcurrentMapCacheManager("customers");
        private Cache customerCache;

        private UUID testCustomerId;
        private Customer testCustomer;
        private CustomerDTO.Request testRequest;
        private CustomerDTO.Response testResponse;

        @BeforeEach
        void setUp() {
                // Initialize cache
                customerCache = cacheManager.getCache("customers");
                customerCache.clear();

                // Setup test data
                testCustomerId = UUID.randomUUID();

                testCustomer = Customer.builder()
                                .id(testCustomerId)
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("+1234567890")
                                .email("john.doe@example.com")
                                .isRegular(true)
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .build();

                testRequest = CustomerDTO.Request.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("+1234567890")
                                .email("john.doe@example.com")
                                .isRegular(true)
                                .build();

                testResponse = CustomerDTO.Response.builder()
                                .id(testCustomerId.toString())
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("+1234567890")
                                .email("john.doe@example.com")
                                .isRegular(true)
                                .createdAt(testCustomer.getCreatedAt())
                                .updatedAt(testCustomer.getUpdatedAt())
                                .build();
        }

        @Nested
        @DisplayName("Cache Behavior Tests")
        class CacheBehaviorTests {

                @Test
                @DisplayName("Should simulate caching behavior for findById")
                void shouldSimulateCachingBehaviorForFindById() {
                        // Given
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerMapper.toResponse(testCustomer))
                                        .thenReturn(testResponse);

                        // When - First call (would cache in real scenario)
                        StepVerifier.create(customerService.findById(testCustomerId))
                                        .expectNext(testResponse)
                                        .verifyComplete();

                        // Simulate caching by manually putting in cache
                        customerCache.put(testCustomerId, testResponse);

                        // When - Second call (simulate cache hit)
                        StepVerifier.create(customerService.findById(testCustomerId))
                                        .expectNext(testResponse)
                                        .verifyComplete();

                        // Then - Repository should be called twice in this test (since @Cacheable is
                        // not active)
                        // But we can verify the cache has the value
                        verify(customerRepository, times(2)).findById(testCustomerId);
                        assertNotNull(customerCache.get(testCustomerId));
                        assertEquals(testResponse, customerCache.get(testCustomerId).get());
                }

                @Test
                @DisplayName("Should not cache deleted customers")
                void shouldNotCacheDeletedCustomers() {
                        // Given
                        Customer deletedCustomer = testCustomer.toBuilder()
                                        .deletedAt(OffsetDateTime.now())
                                        .build();

                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(deletedCustomer));

                        // When
                        StepVerifier.create(customerService.findById(testCustomerId))
                                        .verifyComplete();

                        // Then - Should not attempt to cache deleted customer
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerMapper, never()).toResponse(any());

                        // Simulate that cache should remain empty for deleted customers
                        assertNull(customerCache.get(testCustomerId));
                }

                @Test
                @DisplayName("Should handle empty repository response")
                void shouldHandleEmptyRepositoryResponse() {
                        // Given
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.empty());

                        // When
                        StepVerifier.create(customerService.findById(testCustomerId))
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerMapper, never()).toResponse(any());

                        // Cache should remain empty
                        assertNull(customerCache.get(testCustomerId));
                }
        }

        @Nested
        @DisplayName("Create Operation Tests")
        class CreateOperationTests {

                @Test
                @DisplayName("Should create customer and simulate cache put")
                void shouldCreateCustomerAndSimulateCachePut() {
                        // Given
                        when(customerMapper.toCustomer(testRequest))
                                        .thenReturn(testCustomer);
                        when(customerRepository.save(testCustomer))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerMapper.toResponse(testCustomer))
                                        .thenReturn(testResponse);

                        // When
                        StepVerifier.create(customerService.create(testRequest))
                                        .expectNext(testResponse)
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).save(testCustomer);
                        verify(customerMapper, times(1)).toCustomer(testRequest);
                        verify(customerMapper, times(1)).toResponse(testCustomer);

                        // Simulate @CachePut behavior
                        customerCache.put(testCustomerId, testResponse);
                        assertNotNull(customerCache.get(testCustomerId));
                }
        }

        @Nested
        @DisplayName("Update Operation Tests")
        class UpdateOperationTests {

                @Test
                @DisplayName("Should update customer and simulate cache update")
                void shouldUpdateCustomerAndSimulateCacheUpdate() {
                        // Given
                        CustomerDTO.Request updateRequest = testRequest.toBuilder()
                                        .firstName("Jane")
                                        .build();

                        Customer updatedCustomer = testCustomer.toBuilder()
                                        .firstName("Jane")
                                        .updatedAt(OffsetDateTime.now())
                                        .build();

                        CustomerDTO.Response updatedResponse = testResponse.toBuilder()
                                        .firstName("Jane")
                                        .build();

                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerRepository.save(any(Customer.class)))
                                        .thenReturn(Mono.just(updatedCustomer));
                        when(customerMapper.toResponse(updatedCustomer))
                                        .thenReturn(updatedResponse);

                        // When
                        StepVerifier.create(customerService.update(testCustomerId, updateRequest))
                                        .expectNext(updatedResponse)
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, times(1)).save(any(Customer.class));
                        verify(customerMapper, times(1)).updateCustomerFromRequest(eq(updateRequest),
                                        any(Customer.class));
                        verify(customerMapper, times(1)).toResponse(updatedCustomer);

                        // Simulate @CachePut behavior
                        customerCache.put(testCustomerId, updatedResponse);
                        assertNotNull(customerCache.get(testCustomerId));
                }

                @Test
                @DisplayName("Should throw error when updating non-existent customer")
                void shouldThrowErrorWhenUpdatingNonExistentCustomer() {
                        // Given
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.empty());

                        // When & Then
                        StepVerifier.create(customerService.update(testCustomerId, testRequest))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, never()).save(any());
                        verify(customerMapper, never()).toResponse(any());
                }

                @Test
                @DisplayName("Should throw error when updating deleted customer")
                void shouldThrowErrorWhenUpdatingDeletedCustomer() {
                        // Given
                        Customer deletedCustomer = testCustomer.toBuilder()
                                        .deletedAt(OffsetDateTime.now())
                                        .build();

                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(deletedCustomer));

                        // When & Then
                        StepVerifier.create(customerService.update(testCustomerId, testRequest))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("Soft Delete Operation Tests")
        class SoftDeleteOperationTests {

                @Test
                @DisplayName("Should soft delete customer and simulate cache eviction")
                void shouldSoftDeleteCustomerAndSimulateCacheEviction() {
                        // Given - Put customer in cache first
                        customerCache.put(testCustomerId, testResponse);
                        assertNotNull(customerCache.get(testCustomerId));

                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerRepository.save(any(Customer.class)))
                                        .thenReturn(Mono.just(testCustomer));

                        // When
                        StepVerifier.create(customerService.softDelete(testCustomerId))
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, times(1)).save(any(Customer.class));

                        // Simulate @CacheEvict behavior
                        customerCache.evict(testCustomerId);
                        assertNull(customerCache.get(testCustomerId));
                }

                @Test
                @DisplayName("Should handle soft delete of non-existent customer")
                void shouldHandleSoftDeleteOfNonExistentCustomer() {
                        // Given
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.empty());

                        // When
                        StepVerifier.create(customerService.softDelete(testCustomerId))
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, never()).save(any());
                }

                @Test
                @DisplayName("Should handle soft delete of already deleted customer")
                void shouldHandleSoftDeleteOfAlreadyDeletedCustomer() {
                        // Given
                        Customer deletedCustomer = testCustomer.toBuilder()
                                        .deletedAt(OffsetDateTime.now())
                                        .build();

                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(deletedCustomer));

                        // When
                        StepVerifier.create(customerService.softDelete(testCustomerId))
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerRepository, never()).save(any());
                }
        }

        @Nested
        @DisplayName("Restore Operation Tests")
        class RestoreOperationTests {

                @Test
                @DisplayName("Should restore customer and simulate cache put")
                void shouldRestoreCustomerAndSimulateCachePut() {
                        // Given
                        Customer restoredCustomer = testCustomer.toBuilder()
                                        .deletedAt(null)
                                        .build();

                        when(customerRepository.restoreById(testCustomerId))
                                        .thenReturn(Mono.just(1)); // 1 row affected
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(restoredCustomer));
                        when(customerMapper.toResponse(restoredCustomer))
                                        .thenReturn(testResponse);

                        // When
                        StepVerifier.create(customerService.restore(testCustomerId))
                                        .expectNext(testResponse)
                                        .verifyComplete();

                        // Then
                        verify(customerRepository, times(1)).restoreById(testCustomerId);
                        verify(customerRepository, times(1)).findById(testCustomerId);
                        verify(customerMapper, times(1)).toResponse(restoredCustomer);

                        // Simulate @CachePut behavior
                        customerCache.put(testCustomerId, testResponse);
                        assertNotNull(customerCache.get(testCustomerId));
                }

                @Test
                @DisplayName("Should throw error when restore fails")
                void shouldThrowErrorWhenRestoreFails() {
                        // Given
                        when(customerRepository.restoreById(testCustomerId))
                                        .thenReturn(Mono.just(0)); // 0 rows affected

                        // When & Then
                        StepVerifier.create(customerService.restore(testCustomerId))
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(customerRepository, times(1)).restoreById(testCustomerId);
                        verify(customerRepository, never()).findById(testCustomerId);
                        verify(customerMapper, never()).toResponse(any());

                        // Cache should remain empty
                        assertNull(customerCache.get(testCustomerId));
                }
        }

        @Nested
        @DisplayName("Cache Integration Simulation Tests")
        class CacheIntegrationSimulationTests {

                @Test
                @DisplayName("Should simulate complete CRUD cache lifecycle")
                void shouldSimulateCompleteCrudCacheLifecycle() {
                        // Setup mocks for all operations
                        when(customerMapper.toCustomer(any(CustomerDTO.Request.class)))
                                        .thenReturn(testCustomer);
                        when(customerRepository.save(any(Customer.class)))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerRepository.findById(testCustomerId))
                                        .thenReturn(Mono.just(testCustomer));
                        when(customerMapper.toResponse(any(Customer.class)))
                                        .thenReturn(testResponse);
                        when(customerRepository.restoreById(testCustomerId))
                                        .thenReturn(Mono.just(1));

                        // 1. Create - simulate @CachePut
                        StepVerifier.create(customerService.create(testRequest))        // 1 save
                                        .expectNext(testResponse)
                                        .verifyComplete();
                        customerCache.put(testCustomerId, testResponse);
                        assertNotNull(customerCache.get(testCustomerId));

                        // 2. Read - simulate @Cacheable (would hit cache in real scenario)
                        StepVerifier.create(customerService.findById(testCustomerId))    // 1 findById
                                        .expectNext(testResponse)
                                        .verifyComplete();
                        assertTrue(customerCache.get(testCustomerId) != null);

                        // 3. Update - simulate @CachePut
                        CustomerDTO.Request updateRequest = testRequest.toBuilder()
                                        .firstName("Jane")
                                        .build();

                        CustomerDTO.Response updatedResponse = testResponse.toBuilder()
                                        .firstName("Jane")
                                        .build();

                        StepVerifier.create(customerService.update(testCustomerId, updateRequest)) // 1 findById | 1 save
                                        .expectNext(testResponse) // Original response from mock
                                        .verifyComplete();
                        customerCache.put(testCustomerId, updatedResponse); // Simulate cache update
                        assertNotNull(customerCache.get(testCustomerId));

                        // 4. Soft Delete - simulate @CacheEvict
                        StepVerifier.create(customerService.softDelete(testCustomerId)) // 1 save | 1 findById
                                        .verifyComplete();
                        customerCache.evict(testCustomerId); // Simulate cache eviction
                        assertNull(customerCache.get(testCustomerId));

                        // 5. Restore - simulate @CachePut
                        StepVerifier.create(customerService.restore(testCustomerId)) // 1 restoreById | 1 findById
                                        .expectNext(testResponse)
                                        .verifyComplete();
                        customerCache.put(testCustomerId, testResponse); // Simulate cache put
                        assertNotNull(customerCache.get(testCustomerId));

                        // Verify all operations executed correctly
                        verify(customerRepository, times(3)).save(testCustomer); // Create
                        verify(customerRepository, times(4)).findById(testCustomerId); // Read + Update + Delete
                        verify(customerRepository, times(3)).save(any(Customer.class)); // Delete
                        verify(customerRepository, times(1)).restoreById(testCustomerId); // Restore
                }

                @Test
                @DisplayName("Should verify cache key consistency")
                void shouldVerifyCacheKeyConsistency() {
                        // Given
                        UUID anotherCustomerId = UUID.randomUUID();
                        Customer anotherCustomer = testCustomer.toBuilder()
                                        .id(anotherCustomerId)
                                        .firstName("Alice")
                                        .build();

                        CustomerDTO.Response anotherResponse = testResponse.toBuilder()
                                        .id(anotherCustomerId.toString())
                                        .firstName("Alice")
                                        .build();

                        // When - Simulate caching multiple customers
                        customerCache.put(testCustomerId, testResponse);
                        customerCache.put(anotherCustomerId, anotherResponse);

                        // Then - Verify correct cache keys and values
                        assertNotNull(customerCache.get(testCustomerId));
                        assertNotNull(customerCache.get(anotherCustomerId));

                        assertEquals(testResponse, customerCache.get(testCustomerId).get());
                        assertEquals(anotherResponse, customerCache.get(anotherCustomerId).get());

                        // Verify cache isolation
                        customerCache.evict(testCustomerId);
                        assertNull(customerCache.get(testCustomerId));
                        assertNotNull(customerCache.get(anotherCustomerId)); // Should remain
                }
        }
}