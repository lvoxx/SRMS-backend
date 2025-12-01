package io.github.lvoxx.srms.kitchen.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.models.KitchenOrderItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KitchenOrderItemRepository extends R2dbcRepository<KitchenOrderItem, UUID> {

    Flux<KitchenOrderItem> findByKitchenOrderId(UUID kitchenOrderId);

    Flux<KitchenOrderItem> findByMenuId(UUID menuId);

    @Query("SELECT * FROM kitchen_order_items WHERE kitchen_order_id = :orderId ORDER BY created_at")
    Flux<KitchenOrderItem> findItemsByOrderId(@Param("orderId") UUID orderId);

    @Query("DELETE FROM kitchen_order_items WHERE kitchen_order_id = :orderId")
    Mono<Void> deleteByKitchenOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT COUNT(*) FROM kitchen_order_items WHERE kitchen_order_id = :orderId")
    Mono<Long> countByOrderId(@Param("orderId") UUID orderId);
}