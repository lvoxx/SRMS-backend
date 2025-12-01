package io.github.lvoxx.srms.kitchen.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.models.KitchenMenu;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KitchenMenuRepository extends R2dbcRepository<KitchenMenu, UUID> {

    Flux<KitchenMenu> findByMenuCtgId(UUID categoryId);

    Flux<KitchenMenu> findByIsAvailableTrue();

    Flux<KitchenMenu> findByMenuCtgIdAndIsAvailableTrue(UUID categoryId, Boolean isAvailable);

    @Query("SELECT km.* FROM kitchen_menu km " +
            "WHERE km.menu_ctg_id = :categoryId " +
            "AND km.is_available = true " +
            "ORDER BY km.menu_name")
    Flux<KitchenMenu> findAvailableMenuByCategory(@Param("categoryId") UUID categoryId);

    @Query("SELECT COUNT(*) FROM kitchen_menu WHERE menu_ctg_id = :categoryId")
    Mono<Long> countByCategory(@Param("categoryId") UUID categoryId);
}