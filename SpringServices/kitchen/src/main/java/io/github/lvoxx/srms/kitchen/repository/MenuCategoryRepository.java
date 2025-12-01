package io.github.lvoxx.srms.kitchen.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.models.MenuCategory;
import reactor.core.publisher.Flux;

public interface MenuCategoryRepository extends R2dbcRepository<MenuCategory, UUID> {
    Flux<MenuCategory> findByCtgParentId(UUID parentId);

    Flux<MenuCategory> findByIsActiveTrue();

    @Query("SELECT * FROM menu_category WHERE ctg_parent_id IS NULL AND is_active = true ORDER BY display_order")
    Flux<MenuCategory> findRootCategories();

    @Query("WITH RECURSIVE category_tree AS ( " +
            "    SELECT * FROM menu_category WHERE id = :categoryId " +
            "    UNION ALL " +
            "    SELECT mc.* FROM menu_category mc " +
            "    INNER JOIN category_tree ct ON mc.ctg_parent_id = ct.id " +
            ") SELECT * FROM category_tree")
    Flux<MenuCategory> findCategoryWithChildren(@Param("categoryId") UUID categoryId);
}
