package io.github.lvoxx.srms.kitchen.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.models.KitchenOrder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KitchenOrderRepository extends R2dbcRepository<KitchenOrder, UUID> {

    Flux<KitchenOrder> findByOrderId(UUID orderId);

    Flux<KitchenOrder> findByStatus(String status);

    @Query("SELECT * FROM kitchen_orders WHERE status = :status ORDER BY order_time DESC")
    Flux<KitchenOrder> findByStatusOrderByOrderTimeDesc(@Param("status") String status);

    @Query("SELECT * FROM kitchen_orders " +
            "WHERE order_time >= :startDate AND order_time < :endDate " +
            "ORDER BY order_time DESC")
    Flux<KitchenOrder> findByDateRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT * FROM kitchen_orders " +
            "WHERE status IN ('received', 'preparing') " +
            "ORDER BY order_time ASC")
    Flux<KitchenOrder> findActiveOrders();

    @Query("SELECT COUNT(*) FROM kitchen_orders WHERE status = :status")
    Mono<Long> countByStatus(@Param("status") String status);
}
