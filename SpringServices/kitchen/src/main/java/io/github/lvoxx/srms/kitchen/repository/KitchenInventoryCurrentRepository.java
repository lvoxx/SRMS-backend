package io.github.lvoxx.srms.kitchen.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.models.KitchenInventoryCurrent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KitchenInventoryCurrentRepository extends R2dbcRepository<KitchenInventoryCurrent, UUID> {

    Mono<KitchenInventoryCurrent> findByItemName(String itemName);

    Flux<KitchenInventoryCurrent> findByItemCategory(String itemCategory);

    @Query("SELECT * FROM kitchen_inventory_current " +
            "WHERE min_threshold IS NOT NULL " +
            "AND current_quantity <= min_threshold " +
            "ORDER BY current_quantity ASC")
    Flux<KitchenInventoryCurrent> findLowStockItems();

    @Query("SELECT * FROM kitchen_inventory_current " +
            "WHERE current_quantity > 0 " +
            "ORDER BY item_name")
    Flux<KitchenInventoryCurrent> findInStockItems();

    @Query("UPDATE kitchen_inventory_current " +
            "SET current_quantity = current_quantity + :quantity, " +
            "    last_updated = CURRENT_TIMESTAMP, " +
            "    updated_by = :updatedBy " +
            "WHERE item_name = :itemName")
    Mono<Void> updateQuantity(
            @Param("itemName") String itemName,
            @Param("quantity") Double quantity,
            @Param("updatedBy") String updatedBy);
}