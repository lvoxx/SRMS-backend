package io.github.lvoxx.srms.contactor.services;

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
import io.github.lvoxx.srms.controllerhandler.model.ConflictException;
import io.github.lvoxx.srms.controllerhandler.model.DataPersistantException;
import io.github.lvoxx.srms.controllerhandler.model.InUsedException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.common.utils.CacheValue;
import io.github.lvoxx.srms.common.utils.MessageUtils;
import io.github.lvoxx.srms.contactor.dto.ContactorDTO;
import io.github.lvoxx.srms.contactor.dto.Rating;
import io.github.lvoxx.srms.contactor.mappers.ContactorMapper;
import io.github.lvoxx.srms.contactor.models.Contactor;
import io.github.lvoxx.srms.contactor.models.ContactorType;
import io.github.lvoxx.srms.contactor.repository.ContactorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContactorService {

        private final ContactorRepository contactorRepository;
        private final ContactorMapper contactorMapper;
        private final MessageUtils messageUtils;

        // ==================== CUD Operations ====================

        @CachePut(value = CacheValue.Fields.CONTACTOR, key = "#result.block().id", condition = "#result != null")
        public Mono<ContactorDTO.Response> create(@NonNull ContactorDTO.Request request) {
                log.debug("Creating new contactor with type: {}", request.getType());

                return internalFindAndThrowIfExistedByEmail(request.getEmail())
                                .then(Mono.defer(() -> {
                                        Contactor contactor = contactorMapper.toEntity(request);
                                        return contactorRepository.save(contactor)
                                                        .switchIfEmpty(Mono.error(
                                                                        new DataPersistantException(
                                                                                        messageUtils.getMessage(
                                                                                                        "error.update.failed_to_create",
                                                                                                        new Object[] { request
                                                                                                                        .getEmail() }))))
                                                        .map(contactorMapper::toResponse);
                                }));
        }

        @CachePut(value = CacheValue.Fields.CONTACTOR, key = "#id", condition = "#result != null")
        public Mono<ContactorDTO.Response> update(@NonNull UUID id, @NonNull ContactorDTO.Request request) {
                log.debug("Updating contactor: {}", id);

                return internalAndNotShowDeletedFindById(id)
                                .doOnNext(existing -> contactorMapper.updateEntityFromRequest(request, existing))
                                .flatMap(contactorRepository::save)
                                .switchIfEmpty(Mono.error(new DataPersistantException(messageUtils.getMessage(
                                                "error.update.failed_to_update",
                                                new Object[] { request
                                                                .getEmail() }))))
                                .map(contactorMapper::toResponse);
        }

        @CacheEvict(value = CacheValue.Fields.CONTACTOR, key = "#id")
        public Mono<Boolean> softDelete(@NonNull UUID id) {
                log.debug("Deleting contactor: {}", id);

                return internalAndNotShowDeletedFindById(id)
                                .flatMap(contactor -> contactorRepository.softDeleteById(id))
                                .switchIfEmpty(Mono.error(new DataPersistantException(
                                                messageUtils.getMessage(
                                                                "error.update.failed_to_delete",
                                                                new Object[] { id }))))
                                .map(count -> count > 0);
        }

        @CacheEvict(value = CacheValue.Fields.CONTACTOR, key = "#id")
        public Mono<Boolean> restore(@NonNull UUID id) {
                log.debug("Restoring contactor: {}", id);

                return internalFindByIdForRestoring(id)
                                .flatMap(c -> contactorRepository.restoreById(id))
                                .switchIfEmpty(Mono.error(new DataPersistantException(
                                                messageUtils.getMessage("error.update.failed_to_restore",
                                                                new Object[] { id }))))
                                .map(count -> count > 0);
        }

        // ==================== Query Operations ====================

        @Cacheable(value = CacheValue.Fields.CONTACTOR, key = "#id")
        public Mono<ContactorDTO.Response> findById(@NonNull UUID id) {
                log.debug("Getting contactor with id: {}", id);

                return internalAndNotShowDeletedFindById(id)
                                .map(contactorMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CONTACTOR_EMAIL, key = "#email + ':' + #showDeleted")
        public Mono<ContactorDTO.Response> findByEmail(@NonNull String email, boolean showDeleted) {
                log.debug("Getting contactor with email: {}", email);

                return internalFindByEmailAndShowingDeleted(email.trim().toLowerCase(), showDeleted)
                                .map(contactorMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CONTACTOR_TYPE, key = "#type.name() + ':' + #showDeleted")
        public Flux<ContactorDTO.Response> findByType(@NonNull ContactorType type, boolean showDeleted) {
                log.debug("Getting contactors by type: {}", type);

                return contactorRepository.findByContactTypeAndShowingDeleted(type.name(), showDeleted)
                                .map(contactorMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CONTACTOR_SEARCH, key = "#name + ':' + #showDeleted")
        public Flux<ContactorDTO.Response> findByOrganizationName(@NonNull String name, boolean showDeleted) {
                log.debug("Getting contactors by organization name: {}", name);

                return contactorRepository.findByOrganizationNameContainingAndShowingDeleted(name.trim(), showDeleted)
                                .map(contactorMapper::toResponse);
        }

        @Cacheable(value = CacheValue.Fields.CONTACTOR_PAGE, key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #showDeleted")
        public Mono<PageDTO.Response<ContactorDTO.Response>> findAllContactors(
                        @NonNull PageDTO.Request pageRequest,
                        boolean showDeleted) {
                Pageable pageable = PageDTO.toPagable(pageRequest);

                log.debug("Getting contactors - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

                return contactorRepository.findPageByShowDeleted(pageable, showDeleted)
                                .map(contactorMapper::toResponse)
                                .collectList()
                                // Count total elements
                                .zipWith(contactorRepository.count()
                                                .map(count -> {
                                                        if (showDeleted) {
                                                                return count; // Count all
                                                        }
                                                        return count - contactorRepository.findDeleted()
                                                                        .count()
                                                                        .block(); // Count active only
                                                }))
                                // Map to page response
                                .map(tuple -> {
                                        List<ContactorDTO.Response> content = tuple.getT1();
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

        // ==================== Internal Methods ====================

        private Mono<Void> internalFindAndThrowIfExistedByEmail(String email) {
                return contactorRepository.findByEmail(email, true)
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

        private Mono<Contactor> internalAndNotShowDeletedFindById(UUID id) {
                return contactorRepository.findById(id)
                                .filter(contactor -> !contactor.isDeleted())
                                .switchIfEmpty(
                                                Mono.error(
                                                                new NotFoundException(
                                                                                messageUtils.getMessage(
                                                                                                "error.resource_not_found.id",
                                                                                                new Object[] { id }))));
        }

        private Mono<Contactor> internalFindByIdForRestoring(UUID id) {
                return contactorRepository.findById(id)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new NotFoundException(
                                                                                messageUtils.getMessage(
                                                                                                "error.resource_not_found.id",
                                                                                                new Object[] { id }))))
                                .flatMap(contactor -> {
                                        if (!contactor.isDeleted()) {
                                                return Mono.error(
                                                                new InUsedException(
                                                                                messageUtils.getMessage(
                                                                                                "error.update.inused",
                                                                                                new Object[] { id })));
                                        }
                                        return Mono.just(contactor);
                                });
        }

        private Mono<Contactor> internalFindByEmailAndShowingDeleted(String email, boolean showDeleted) {
                return contactorRepository.findByEmail(email, showDeleted)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new NotFoundException(
                                                                                messageUtils.getMessage(
                                                                                                "error.resource_not_found.email",
                                                                                                new Object[] { email }))));
        }

        public Mono<Boolean> existsByEmail(String email) {
                if (email == null || email.trim().isEmpty()) {
                        return Mono.just(false);
                }
                return findByEmail(email, false).hasElement().onErrorReturn(false);
        }

        public Mono<Boolean> existsByPhoneNumber(String phoneNumber) {
                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                        return Mono.just(false);
                }
                return contactorRepository.findAllByShowingDeleted(false)
                                .any(contactor -> phoneNumber.equals(contactor.getPhoneNumber()))
                                .onErrorReturn(false);
        }

        public Mono<Long> countByType(ContactorType type) {
                return findByType(type, false).count().onErrorReturn(0L);
        }

        public Flux<ContactorDTO.Response> getHighRatingContactors() {
                return contactorRepository.findAllByShowingDeleted(false)
                                .filter(contactor -> Rating.HIGH.getRate().equals(contactor.getRating()))
                                .map(contactorMapper::toResponse)
                                .onErrorResume(ex -> {
                                        log.error("Error getting high rating contactors", ex);
                                        return Flux.empty();
                                });
        }

        public Flux<ContactorDTO.Response> getDeletedContactors() {
                return contactorRepository.findDeleted()
                                .map(contactorMapper::toResponse)
                                .onErrorResume(ex -> {
                                        log.error("Error getting deleted contactors", ex);
                                        return Flux.empty();
                                });
        }

        @CacheEvict(value = { CacheValue.Fields.CONTACTOR, CacheValue.Fields.CONTACTOR_PAGE,
                        CacheValue.Fields.CONTACTOR_TYPE, CacheValue.Fields.CONTACTOR_SEARCH,
                        CacheValue.Fields.CONTACTOR_EMAIL }, allEntries = true)
        public void clearAllCache() {
                log.info("Clearing all contactor caches");
        }
}