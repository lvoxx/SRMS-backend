package io.github.lvoxx.srms.customer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.github.lvoxx.srms.customer.models.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, String> {

    // Lấy tất cả kể cả đã xóa
    @Query("SELECT * FROM customer")
    Flux<Customer> findAllIncludingDeleted();

    // Lấy các record đã bị soft delete
    @Query("SELECT * FROM customer WHERE deleted_at IS NOT NULL")
    Flux<Customer> findDeleted();

    // Khôi phục soft delete
    @Query("UPDATE customer SET deleted_at = NULL WHERE id = :id")
    Mono<Integer> restoreById(String id);

    // Lấy các record chưa bị soft delete với phân trang
    @Query("SELECT * FROM customer WHERE deleted_at IS NULL")
    Flux<Customer> findAllActive(Pageable pageable);
}
