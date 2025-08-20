package io.github.lvoxx.srms.customer.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Cacheable(value = "customers", key = "#id", unless = "#result == null || #result.deletedAt != null")
    public Mono<CustomerDTO.Response> findById(UUID id) {
        return customerRepository.findById(id)
                .filter(customer -> customer.getDeletedAt() == null) // Only cache active records
                .map(customerMapper::toResponse);
    }

    @SuppressWarnings("null") // Just skip it, PageDTO record already check null
    public Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> findAllPaged(
            PageDTO.PageRequestDTO pageRequest, boolean showDeleted) {
        Pageable pageable = PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(pageRequest.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                        pageRequest.sortBy()));

        return customerRepository.findPageByShowDeleted(pageable, showDeleted)
                .map(customerMapper::toResponse)
                .collectList()
                // Count total elements
                .zipWith(customerRepository.count()
                        .map(count -> {
                            if (showDeleted) {
                                return count; // Count all
                            }
                            return count - customerRepository.findDeleted().count().block(); // Count active only
                        }))
                // Map to page response
                .map(tuple -> {
                    List<CustomerDTO.Response> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
                    return new PageDTO.PageResponseDTO<>(
                            content,
                            pageRequest.page(),
                            pageRequest.size(),
                            totalElements,
                            totalPages);
                });
    }

    @CachePut(value = "customers", key = "#result.id")
    public Mono<CustomerDTO.Response> create(CustomerDTO.Request request) {
        Customer customer = customerMapper.toCustomer(request);
        return customerRepository.save(customer)
                .map(customerMapper::toResponse);
    }

    @CachePut(value = "customers", key = "#id")
    public Mono<CustomerDTO.Response> update(UUID id, CustomerDTO.Request request) {
        return customerRepository.findById(id)
                .filter(existing -> existing.getDeletedAt() == null) // Only update active
                .switchIfEmpty(Mono.error(new RuntimeException("Customer not found or deleted")))
                .map(existing -> {
                    customerMapper.updateCustomerFromRequest(request, existing);
                    return existing;
                })
                .flatMap(customerRepository::save)
                .map(customerMapper::toResponse);
    }

    @CacheEvict(value = "customers", key = "#id")
    public Mono<Void> softDelete(UUID id) {
        return customerRepository.findById(id)
                .filter(existing -> existing.getDeletedAt() == null)
                .switchIfEmpty(Mono.empty())
                .map(existing -> {
                    existing.setDeletedAt(OffsetDateTime.now());
                    return existing;
                })
                .flatMap(customerRepository::save)
                .then();
    }

    @CachePut(value = "customers", key = "#id")
    public Mono<CustomerDTO.Response> restore(UUID id) {
        return customerRepository.restoreById(id)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new RuntimeException("No customer to restore")))
                .flatMap(rows -> customerRepository.findById(id))
                .map(customerMapper::toResponse);
    }

    // For performance issue, restrict to use this. Use paging instead.
    @SuppressWarnings("unused")
    private Flux<CustomerDTO.Response> findAllWithShowDeleted(boolean showDeleted) {
        // No caching for lists to avoid complexity with invalidation
        return customerRepository.findAllByShowDeleted(showDeleted)
                .map(customerMapper::toResponse);
    }

    @SuppressWarnings("unused")
    // For performance issue, restrict to use this. Use paging instead
    private Flux<CustomerDTO.Response> findDeleted() {
        // No caching
        return customerRepository.findDeleted()
                .map(customerMapper::toResponse);
    }
}
