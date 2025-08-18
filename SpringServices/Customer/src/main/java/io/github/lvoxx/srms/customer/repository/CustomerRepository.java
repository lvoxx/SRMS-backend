package io.github.lvoxx.srms.customer.repository;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.customer.models.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends R2dbcRepository<Customer, UUID> {

        // Find By ... And showDeleted flag
        @Query("SELECT * FROM Customer c WHERE " +
                        "((:showDeleted = true AND c.deleted_at IS NOT NULL) OR (:showDeleted = false AND c.deleted_at IS NULL))")
        Flux<Customer> findAllByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // Find by email excluding deleted
        @Query("SELECT * FROM Customer c WHERE c.email = :email AND " +
                        "((:showDeleted = true AND c.deleted_at IS NOT NULL) OR (:showDeleted = false AND c.deleted_at IS NULL))")
        Mono<Customer> findActiveByEmailAndShowDeleted(@Param("email") String email,
                        @Param("showDeleted") boolean showDeleted);

        // Find by phone number excluding deleted
        @Query("SELECT * FROM Customer c WHERE c.phone_number = :phoneNumber AND " +
                        "((:showDeleted = true AND c.deleted_at IS NOT NULL) OR (:showDeleted = false AND c.deleted_at IS NULL))")
        Mono<Customer> findActiveByPhoneNumberAndShowDeleted(@Param("phoneNumber") String phoneNumber,
                        @Param("showDeleted") boolean showDeleted);

        // Find regular customers
        @Query("SELECT * FROM Customer c WHERE c.is_regular = true AND " +
                        "((:showDeleted = true AND c.deleted_at IS NOT NULL) OR (:showDeleted = false AND c.deleted_at IS NULL))")
        Flux<Customer> findActiveRegularCustomersByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // -------------------------------------------------------------------

        // Lấy các record đã bị soft delete
        @Query("SELECT * FROM Customer c WHERE c.deleted_at IS NOT NULL")
        Flux<Customer> findDeleted();

        // Khôi phục soft delete
        @Query("UPDATE Customer c SET c.deleted_at = NULL WHERE c.id = :id")
        Mono<Integer> restoreById(@Param("id") UUID id);

        // Lấy các record chưa bị soft delete với phân trang
        // Note: Fixed 'SELECT c FROM' to 'SELECT * FROM' for consistency; Spring Data
        // R2DBC will append LIMIT/OFFSET for Pageable
        @Query("SELECT * FROM Customer c WHERE " +
                        "((:showDeleted = true AND c.deleted_at IS NOT NULL) OR (:showDeleted = false AND c.deleted_at IS NULL))")
        Flux<Customer> findPageByShowDeleted(Pageable pageable, @Param("showDeleted") boolean showDeleted);
}