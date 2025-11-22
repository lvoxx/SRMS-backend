package io.github.lvoxx.srms.warehouse.repositories;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.srms.warehouse.models.Warehouse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WarehouseRepository extends R2dbcRepository<Warehouse, UUID> {

        // ==================== FIND BY ID ====================

        @Query("""
                        SELECT * FROM warehouse
                        WHERE id = :id
                        AND (:includeDeleted = true OR is_deleted = false)
                        """)
        Mono<Warehouse> findById(
                        @Param("id") UUID id,
                        @Param("includeDeleted") boolean includeDeleted);

        // ==================== FIND BY NAME ====================

        @Query("""
                        SELECT * FROM warehouse
                        WHERE LOWER(product_name) = LOWER(:productName)
                        AND (:includeDeleted = true OR is_deleted = false)
                        """)
        Mono<Warehouse> findByProductName(
                        @Param("productName") String productName,
                        @Param("includeDeleted") boolean includeDeleted);

        // ==================== FIND ALL WITH FILTERS ====================
        @Query("""
                        SELECT * FROM warehouse
                        WHERE (:includeDeleted = true OR is_deleted = false)
                        AND (:productName IS NULL OR LOWER(product_name) LIKE LOWER(CONCAT('%', :productName, '%')))
                        AND (:minQuantity IS NULL OR quantity >= :minQuantity)
                        AND (:maxQuantity IS NULL OR quantity <= :maxQuantity)
                        AND (:createdFrom IS NULL OR created_at >= :createdFrom)
                        AND (:createdTo IS NULL OR created_at <= :createdTo)
                        AND (:updatedFrom IS NULL OR updated_at >= :updatedFrom)
                        AND (:updatedTo IS NULL OR updated_at <= :updatedTo)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<Warehouse> findAllWithFilters(
                        @Param("includeDeleted") boolean includeDeleted,
                        @Param("productName") String productName,
                        @Param("minQuantity") Integer minQuantity,
                        @Param("maxQuantity") Integer maxQuantity,
                        @Param("createdFrom") OffsetDateTime createdFrom,
                        @Param("createdTo") OffsetDateTime createdTo,
                        @Param("updatedFrom") OffsetDateTime updatedFrom,
                        @Param("updatedTo") OffsetDateTime updatedTo,
                        Pageable pageable);

        // ==================== FIND ALL SIMPLE ====================
        @Query("""
                        SELECT * FROM warehouse
                        WHERE (:includeDeleted = true OR is_deleted = false)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<Warehouse> findAll(
                        @Param("includeDeleted") boolean includeDeleted,
                        Pageable pageable);

        // ==================== FIND PRODUCTS BELOW MINIMUM ====================
        @Query("""
                        SELECT * FROM warehouse
                        WHERE quantity < min_quantity
                        AND is_deleted = false
                        ORDER BY (min_quantity - quantity) DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<Warehouse> findProductsBelowMinimum(Pageable pageable);

        // ==================== FIND OUT OF STOCK ====================
        @Query("""
                        SELECT * FROM warehouse
                        WHERE quantity = 0
                        AND is_deleted = false
                        ORDER BY updated_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<Warehouse> findOutOfStock(Pageable pageable);

        // ==================== UPDATE (WITHOUT QUANTITY) ====================

        @Modifying
        @Query("""
                        UPDATE warehouse
                        SET product_name = COALESCE(:productName, product_name),
                            min_quantity = COALESCE(:minQuantity, min_quantity),
                            contactor_id = :contactorId,
                            updated_at = :updatedAt,
                            last_updated_by = :lastUpdatedBy,
                            version = version + 1
                        WHERE id = :id
                        AND is_deleted = false
                        """)
        Mono<Integer> updateWarehouse(
                        @Param("id") UUID id,
                        @Param("productName") String productName,
                        @Param("minQuantity") Integer minQuantity,
                        @Param("contactorId") UUID contactorId,
                        @Param("updatedAt") OffsetDateTime updatedAt,
                        @Param("lastUpdatedBy") String lastUpdatedBy);

        // ==================== SOFT DELETE ====================

        @Modifying
        @Query("""
                        UPDATE warehouse
                        SET is_deleted = true,
                            updated_at = :updatedAt,
                            last_updated_by = :lastUpdatedBy
                        WHERE id = :id
                        AND is_deleted = false
                        """)
        Mono<Integer> softDelete(
                        @Param("id") UUID id,
                        @Param("updatedAt") OffsetDateTime updatedAt,
                        @Param("lastUpdatedBy") String lastUpdatedBy);

        // ==================== RESTORE ====================

        @Modifying
        @Query("""
                        UPDATE warehouse
                        SET is_deleted = false,
                            updated_at = :updatedAt,
                            last_updated_by = :lastUpdatedBy
                        WHERE id = :id
                        AND is_deleted = true
                        """)
        Mono<Integer> restore(
                        @Param("id") UUID id,
                        @Param("updatedAt") OffsetDateTime updatedAt,
                        @Param("lastUpdatedBy") String lastUpdatedBy);

        // ==================== COUNT ====================

        @Query("""
                        SELECT COUNT(*) FROM warehouse
                        WHERE (:includeDeleted = true OR is_deleted = false)
                        """)
        Mono<Long> countAll(@Param("includeDeleted") boolean includeDeleted);

        @Query("""
                        SELECT COUNT(*) FROM warehouse
                        WHERE quantity < min_quantity
                        AND is_deleted = false
                        """)
        Mono<Long> countBelowMinimum();

        @Query("""
                        SELECT COUNT(*) FROM warehouse
                        WHERE quantity = 0
                        AND is_deleted = false
                        """)
        Mono<Long> countOutOfStock();
}