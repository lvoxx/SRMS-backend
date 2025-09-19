package io.github.lvoxx.srms.customer.services;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.common.exception.model.ConflictException;
import io.github.lvoxx.srms.common.exception.model.DataPersistantException;
import io.github.lvoxx.srms.common.exception.model.InUsedException;
import io.github.lvoxx.srms.common.exception.model.NotFoundException;
import io.github.lvoxx.srms.common.utils.CacheValue;
import io.github.lvoxx.srms.common.utils.MessageUtils;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.mappers.CustomerMapper;
import io.github.lvoxx.srms.customer.models.Customer;
import io.github.lvoxx.srms.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
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
                return internalFindPhoneNumber(phoneNumber)
                                .map(customerMapper::toResponse);
        }

        public Mono<PageDTO.Response<CustomerDTO.Response>> findAllPaged(
                        PageDTO.Request pageRequest, boolean showDeleted) {
                Pageable pageable = PageDTO.toPagable(pageRequest);

                return customerRepository.findPageByShowDeleted(pageable, showDeleted)
                                .map(customerMapper::toResponse)
                                .collectList()
                                // Count total elements
                                .zipWith(customerRepository.count()
                                                .map(count -> {
                                                        if (showDeleted) {
                                                                return count; // Count all
                                                        }
                                                        return count - customerRepository.findDeleted()
                                                                        .count()
                                                                        .block(); // Count active only
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
        public Mono<CustomerDTO.Response> create(@NonNull CustomerDTO.Request request) {
                log.debug("Creating new contactor with fullname: {}",
                                request.getFirstName() + " " + request.getLastName());

                return internalFindAndThrowIfExistedByEmail(request.getEmail())
                                .then(Mono.defer(() -> {
                                        Customer customer = customerMapper.toCustomer(request);
                                        return customerRepository.save(customer)
                                                        .switchIfEmpty(Mono.error(
                                                                        new DataPersistantException(
                                                                                        messageUtils.getMessage(
                                                                                                        "error.update.failed_to_create",
                                                                                                        new Object[] { request
                                                                                                                        .getEmail() }))))
                                                        .map(customerMapper::toResponse);
                                }));
        }

        @CachePut(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<CustomerDTO.Response> update(UUID id, @NonNull CustomerDTO.Request request) {
                log.debug("Updating customer with id: {}", id);

                return internalFindActiveById(id)
                                .doOnNext(existing -> customerMapper.updateCustomerFromRequest(request, existing))
                                .flatMap(customerRepository::save)
                                .switchIfEmpty(Mono.error(new DataPersistantException(
                                                messageUtils.getMessage(
                                                                "error.update.failed_to_update",
                                                                new Object[] { request
                                                                                .getEmail() }))))
                                .map(customerMapper::toResponse);
        }

        @CacheEvict(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<Boolean> softDelete(UUID id) {
                log.debug("Deleting customer: {}", id);

                return internalFindActiveById(id)
                                .flatMap(customer -> customerRepository.softDeleteById(customer.getId()))
                                .switchIfEmpty(Mono.error(new DataPersistantException(
                                                messageUtils.getMessage("error.update.failed_to_delete",
                                                                new Object[] { id }))))
                                .map(count -> count > 0);
        }

        @CachePut(value = CacheValue.Fields.CUSTOMERS, key = "#id")
        public Mono<Boolean> restore(UUID id) {
                log.debug("Restoring customer: {}", id);

                return internalFindByIdForRestoring(id)
                                .flatMap(c -> customerRepository.restoreById(id))
                                .switchIfEmpty(Mono.error(new DataPersistantException(
                                                messageUtils.getMessage("error.update.failed_to_restore",
                                                                new Object[] { id }))))
                                .map(count -> count > 0);
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

        // ==================== Internal Methods ====================

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

        private Mono<Customer> internalFindByIdForRestoring(UUID id) {
                return customerRepository.findActiveById(id)
                                .switchIfEmpty(
                                                Mono.error(new NotFoundException(
                                                                messageUtils.getMessage(
                                                                                "error.resource_not_found.active_id",
                                                                                new Object[] { id }))))
                                .flatMap(customer -> {
                                        if (!customer.isDeleted()) {
                                                return Mono.error(
                                                                new InUsedException(
                                                                                messageUtils.getMessage(
                                                                                                "error.update.inused",
                                                                                                new Object[] { id })));
                                        }
                                        return Mono.just(customer);
                                });
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

        private Mono<Customer> internalFindPhoneNumber(String phoneNumber) {
                return customerRepository.findByPhoneNumber(phoneNumber)
                                .switchIfEmpty(Mono.error(
                                                new NotFoundException(messageUtils.getMessage(
                                                                "error.resource_not_found.phone_number",
                                                                new Object[] { phoneNumber }))));
        }
}
