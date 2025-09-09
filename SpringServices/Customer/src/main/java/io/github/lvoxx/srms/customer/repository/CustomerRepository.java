package io.github.lvoxx.srms.customer.repository;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.customer.models.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends R2dbcRepository<Customer, UUID> {

        // Find By ... And showDeleted flag
        @Query("SELECT * FROM customer WHERE " +
                        "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
        Flux<Customer> findAllByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // Find by email excluding deleted
        @Query("SELECT * FROM customer WHERE id = :id AND deleted_at IS NULL")
        Mono<Customer> findActiveById(@Param("id") UUID id);

        // Find by email excluding deleted
        @Query("SELECT * FROM customer WHERE email = :email AND " +
                        "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
        Mono<Customer> findActiveByEmailAndShowDeleted(@Param("email") String email,
                        @Param("showDeleted") boolean showDeleted);

        // Find by phone number excluding deleted
        @Query("SELECT * FROM customer WHERE phone_number = :phoneNumber AND " +
                        "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
        Mono<Customer> findActiveByPhoneNumberAndShowDeleted(@Param("phoneNumber") String phoneNumber,
                        @Param("showDeleted") boolean showDeleted);

        // Find regular customers
        @Query("SELECT * FROM customer WHERE is_regular = true AND " +
                        "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
        Flux<Customer> findActiveRegularCustomersByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // -------------------------------------------------------------------

        // Lấy các record đã bị soft delete
        @Modifying
        @Query("SELECT * FROM customer WHERE deleted_at IS NOT NULL")
        Flux<Customer> findDeleted();

        // Soft delete by ID
        @Modifying
        @Query("UPDATE customer SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id AND deleted_at IS NULL")
        Mono<Integer> softDeleteById(@Param("id") UUID id);

        // Khôi phục soft delete
        @Modifying
        @Query("UPDATE customer SET deleted_at = NULL WHERE id = :id")
        Mono<Integer> restoreById(@Param("id") UUID id);

        // Lấy các record chưa bị soft delete với phân trang
        // Note: Fixed 'SELECT c FROM' to 'SELECT * FROM' for consistency; Spring Data
        // R2DBC will append LIMIT/OFFSET for Pageable
        @Query("SELECT * FROM customer WHERE " +
                        "((:showDeleted = true AND deleted_at IS NOT NULL) OR (:showDeleted = false AND deleted_at IS NULL))")
        Flux<Customer> findPageByShowDeleted(Pageable pageable, @Param("showDeleted") boolean showDeleted);

        // -------------------------------------------------------------------

        // FOR INTERNAL CALL ONLY
        @Query("SELECT * FROM customer WHERE CONCAT(first_name, ' ', last_name) LIKE '%' || :fullName || '%'")
        Flux<Customer> findAlikeFullname(@Param("fullName") String fullName);

        @Query("SELECT * FROM customer WHERE email = :email")
        Mono<Customer> findByEmail(@Param("email") String email);

        @Query("SELECT * FROM customer WHERE phone_number = :phoneNumber")
        Mono<Customer> findByPhoneNumber(@Param("phonenumber") String phoneNumber);
}