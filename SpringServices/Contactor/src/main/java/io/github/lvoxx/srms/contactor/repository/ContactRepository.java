package io.github.lvoxx.srms.contactor.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.lvoxx.srms.contactor.models.Contactor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContactRepository extends ReactiveCrudRepository<Contactor, UUID> {

    // Find all non-deleted contacts
    @Query("SELECT * FROM contactor WHERE deleted_at IS NULL")
    Flux<Contactor> findAllActive();

    // Find by ID, excluding deleted
    @Query("SELECT * FROM contactor WHERE id = :id AND deleted_at IS NULL")
    Mono<Contactor> findActiveById(UUID id);

    // Soft delete by ID
    @Query("UPDATE contactor SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    Mono<Integer> softDeleteById(UUID id);

    // Find by contact type, excluding deleted
    @Query("SELECT * FROM contactor WHERE contact_type = :type AND deleted_at IS NULL")
    Flux<Contactor> findByContactType(String type);

    // Find by organization name (partial match), excluding deleted
    @Query("SELECT * FROM contactor WHERE organization_name ILIKE %:name% AND deleted_at IS NULL")
    Flux<Contactor> findByOrganizationNameContaining(String name);

    // Find by email, excluding deleted
    @Query("SELECT * FROM contactor WHERE email = :email AND deleted_at IS NULL")
    Mono<Contactor> findByEmail(String email);
}