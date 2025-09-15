package io.github.lvoxx.srms.customer.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.common.exception.model.ConflictException;
import io.github.lvoxx.srms.common.exception.model.DataPersistantException;
import io.github.lvoxx.srms.common.exception.model.NotFoundException;
import io.github.lvoxx.srms.common.utils.CacheValue;
import io.github.lvoxx.srms.common.utils.MessageUtils;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {
        private final CustomerRepository customerRepository;
        private final CustomerMapper customerMapper;
        private final MessageUtils messageUtils;

        @Cacheable(value = CacheValue.Fields.CUSTOMERS, key = "#id", unless = "#result == null")
        public Mono<CustomerDTO.Response> findById(UUID id) {
                return internalFindById(id)
                                .map(customerMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CUSTOMERS, key = "#email", unless = "#result == null")
        public Mono<CustomerDTO.Response> findByEmail(String email) {
                return internalFindByEmail(email)
                                .map(customerMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CUSTOMERS, key = "#fullName", unless = "#result == null")
        public Flux<CustomerDTO.Response> findAlikeFullName(String fullName) {
                return internalFindAlikeFullName(fullName)
                                .map(customerMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CUSTOMERS, key = "#phoneNumber", unless = "#result == null")
        public Mono<CustomerDTO.Response> findByPhoneNumber(String phoneNumber) {
                return customerRepository.findByPhoneNumber(phoneNumber)
                                .switchIfEmpty(Mono.error(
                                                new NotFoundException(messageUtils.getMessage(
                                                                "error.resource_not_found.phone_number",
                                                                new Object[] { phoneNumber }))))
                                .map(customerMapper::toResponse);
        }

        @SuppressWarnings("null") // Just skip it, PageDTO record already check null
        public Mono<PageDTO.Response<CustomerDTO.Response>> findAllPaged(
                        PageDTO.Request pageRequest, boolean showDeleted) {
                Pageable pageable = PageRequest.of(
                                pageRequest.page(),
                                pageRequest.size(),
                                Sort.by(pageRequest.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC
                                                : Sort.Direction.DESC,
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
                                                        return count - customerRepository.findDeleted().count().block(); // Count
                                                                                                                         // active
                                                                                                                         // only
                                                }))
                                // Map to page response
                                .map(tuple -> {
                                        List<CustomerDTO.Response> content = tuple.getT1();
                                        long totalElements = tuple.getT2();
                                        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
                                        return new PageDTO.Response<>(
                                                        content,
                                                        pageRequest.page(),
                                                        pageRequest.size(),
                                                        totalElements,
                                                        totalPages);
                                });
        }

        @CachePut(value = CacheValue.Fields.CUSTOMERS, key = "#result.block().id", condition = "#result != null")
        public Mono<CustomerDTO.Response> create(CustomerDTO.Request request) {
                // Check for null
                if (!Optional.ofNullable(request).isPresent()) {
                        return Mono.error(new NullPointerException(
                                        messageUtils.getMessage("error.body.null",
                                                        new Object[] {})));
                }
                return internalFindAndThrowIfExistedByEmail(request.getEmail())
                                .then(Mono.defer(() -> {
                                        Customer customer = customerMapper.toCustomer(request);
                                        return customerRepository.save(customer)
                                                        .switchIfEmpty(Mono.error(new DataPersistantException()))
                                                        .map(customerMapper::toResponse)
                                                        .onErrorMap(ex -> !(ex instanceof ConflictException),
                                                                        ex -> new DataPersistantException(
                                                                                        messageUtils.getMessage(
                                                                                                        "error.update.failed_to_create",
                                                                                                        new Object[] { request
                                                                                                                        .getEmail() })));
                                }));
        }

        @CachePut(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<CustomerDTO.Response> update(UUID id, CustomerDTO.Request request) {
                // 1. Check for null
                if (!Optional.ofNullable(request).isPresent()) {
                        return Mono.error(new NullPointerException(
                                        messageUtils.getMessage("error.body.null",
                                                        new Object[] {})));
                }
                // 2. Then check for customer is existed
                return internalFindActiveById(id)
                                .switchIfEmpty(Mono.error(new DataPersistantException()))
                                // 3. If do exists, then update
                                .map(existing -> {
                                        customerMapper.updateCustomerFromRequest(request, existing);
                                        return existing;
                                })
                                .flatMap(customerRepository::save)
                                .switchIfEmpty(Mono.error(new DataPersistantException()))
                                .onErrorMap(ex -> !(ex instanceof NotFoundException),
                                                ex -> new DataPersistantException(
                                                                messageUtils.getMessage(
                                                                                "error.update.failed_to_update",
                                                                                new Object[] { request
                                                                                                .getEmail() })))
                                .map(customerMapper::toResponse);
        }

        @CacheEvict(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<Void> softDelete(UUID id) {
                return internalFindActiveById(id)
                                .switchIfEmpty(Mono.error(new NotFoundException(
                                                messageUtils.getMessage("error.resource_not_found.active_id",
                                                                new Object[] { id }))))
                                .map(existing -> {
                                        existing.setDeletedAt(OffsetDateTime.now());
                                        return existing;
                                })
                                .flatMap(customerRepository::save)
                                .switchIfEmpty(Mono.error(new DataPersistantException()))
                                // Filter to not consume NotFoundException
                                .onErrorMap(ex -> !(ex instanceof NotFoundException),
                                                ex -> new DataPersistantException(
                                                                messageUtils.getMessage("error.update.failed_to_delete",
                                                                                new Object[] { id })))

                                .then();
        }

        @CachePut(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<CustomerDTO.Response> restore(UUID id) {
                return customerRepository.restoreById(id)
                                .flatMap(rows -> {
                                        if (rows == 0) {
                                                return Mono.error(new DataPersistantException(
                                                                messageUtils.getMessage(
                                                                                "error.update.failed_to_restore",
                                                                                new Object[] { id })));
                                        }
                                        return internalFindById(id);
                                })
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

        private Mono<Customer> internalFindById(UUID id) {
                return customerRepository.findById(id)
                                .switchIfEmpty(
                                                Mono.error(new NotFoundException(
                                                                messageUtils.getMessage("error.resource_not_found.id",
                                                                                new Object[] { id }))));
        }

        private Mono<Customer> internalFindActiveById(UUID id) {
                return customerRepository.findActiveById(id)
                                .switchIfEmpty(
                                                Mono.error(new NotFoundException(
                                                                messageUtils.getMessage(
                                                                                "error.resource_not_found.active_id",
                                                                                new Object[] { id }))));
        }

        private Mono<Customer> internalFindByEmail(String email) {
                return customerRepository.findByEmail(email)
                                .switchIfEmpty(Mono
                                                .error(new NotFoundException(
                                                                messageUtils.getMessage(
                                                                                "error.resource_not_found.email",
                                                                                new Object[] { email }))));
        }

        private Mono<Void> internalFindAndThrowIfExistedByEmail(String email) {
                return customerRepository.findByEmail(email)
                                .hasElement()
                                .flatMap(exists -> {
                                        if (exists) {
                                                return Mono.error(new ConflictException(
                                                                messageUtils.getMessage(
                                                                                "error.update.conflicted",
                                                                                new Object[] { email })));
                                        }
                                        return Mono.empty(); // Hoặc xử lý logic khi không tồn tại
                                }).then();
        }

        private Flux<Customer> internalFindAlikeFullName(String fullName) {
                return customerRepository.findAlikeFullname(fullName);
        }
}
