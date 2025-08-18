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
        @Query("SELECT * FROM Customer c WHERE c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Flux<Customer> findAllByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // Find by ID excluding deleted
        @Query("SELECT * FROM Customer c WHERE c.id = :id AND c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Mono<Customer> findCustomerByIdAndShowDeleted(@Param("id") UUID id, @Param("showDeleted") boolean showDeleted);

        // Find by email excluding deleted
        @Query("SELECT * FROM Customer c WHERE c.email = :email AND c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Mono<Customer> findActiveByEmailAndShowDeleted(@Param("email") String email, @Param("showDeleted") boolean showDeleted);

        // Find by phone number excluding deleted
        @Query("SELECT * FROM Customer c WHERE c.phoneNumber = :phoneNumber AND c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Mono<Customer> findActiveByPhoneNumberAndShowDeleted(@Param("phoneNumber") String phoneNumber,
                        @Param("showDeleted") boolean showDeleted);

        // Find regular customers
        @Query("SELECT * FROM Customer c WHERE c.isRegular = true AND c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Flux<Customer> findActiveRegularCustomersByShowDeleted(@Param("showDeleted") boolean showDeleted);

        // -------------------------------------------------------------------

        // Lấy các record đã bị soft delete
        @Query("SELECT * FROM Customer c WHERE c.deletedAt IS NOT NULL")
        Flux<Customer> findDeleted();

        // Khôi phục soft delete
        @Query("UPDATE Customer c SET c.deletedAt = NULL WHERE c.id = :id")
        Mono<Integer> restoreById(UUID id);

        // Lấy các record chưa bị soft delete với phân trang
        @Query("SELECT c FROM Customer c WHERE c.deletedAt IS " +
                        "CASE WHEN :showDeleted = true THEN NOT NULL ELSE NULL END")
        Flux<Customer> findPageByIsDeleted(Pageable pageable, @Param("showDeleted") boolean showDeleted);
}
