package io.github.lvoxx.srms.contactor.repository;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.lvoxx.srms.contactor.models.Contactor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContactorRepository extends ReactiveCrudRepository<Contactor, UUID> {

    // Find all
    @Query("SELECT * FROM contactor WHERE " +
            "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
    Flux<Contactor> findAllByShowingDeleted(@Param("showDeleted") boolean showDeleted);

    @Query("SELECT * FROM contactor WHERE " +
            "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
    Flux<Contactor> findPageByShowDeleted(Pageable pageable, @Param("showDeleted") boolean showDeleted);

    // Find by contact type, excluding deleted
    @Query("SELECT * FROM contactor WHERE contactor_type = :type AND " +
            "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
    Flux<Contactor> findByContactTypeAndShowingDeleted(@Param("type") String type,
            @Param("showDeleted") boolean showDeleted);

    // Find by organization name (partial match), excluding deleted
    @Query("SELECT * FROM contactor WHERE organization_name ILIKE CONCAT('%', :name, '%') AND " +
            "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
    Flux<Contactor> findByOrganizationNameContainingAndShowingDeleted(@Param("name") String name,
            @Param("showDeleted") boolean showDeleted);

    // Find by email, excluding deleted
    @Query("SELECT * FROM contactor WHERE email = :email AND " +
            "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
    Mono<Contactor> findByEmail(@Param("email") String email, @Param("showDeleted") boolean showDeleted);

    // -------------------------------------------------------------------

    // Lấy các record đã bị soft delete
    @Query("SELECT * FROM contactor WHERE deleted_at IS NOT NULL")
    Flux<Contactor> findDeleted();

    // Soft delete by ID
    @Modifying
    @Query("UPDATE contactor SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
    Mono<Integer> softDeleteById(@Param("id") UUID id);

    // Khôi phục soft delete
    @Modifying
    @Query("UPDATE contactor SET deleted_at = NULL WHERE id = :id")
    Mono<Integer> restoreById(@Param("id") UUID id);
}