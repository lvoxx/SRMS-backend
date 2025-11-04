package io.github.lvoxx.srms.warehouse.repositories;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.srms.warehouse.models.WarehouseHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WarehouseHistoryRepository extends R2dbcRepository<WarehouseHistory, UUID> {

        // ==================== FIND ALL WITH FILTERS ====================
        @Query("""
                        SELECT wh.*
                        FROM warehouse_history wh
                        LEFT JOIN warehouse w ON wh.warehouse_id = w.id
                        WHERE (:warehouseId IS NULL OR wh.warehouse_id = :warehouseId)
                        AND (:type IS NULL OR wh.type = CAST(:type AS TEXT))
                        AND (:productName IS NULL OR LOWER(w.product_name) LIKE LOWER(CONCAT('%', :productName, '%')))
                        AND (:minQuantity IS NULL OR wh.quantity >= :minQuantity)
                        AND (:maxQuantity IS NULL OR wh.quantity <= :maxQuantity)
                        AND (:createdFrom IS NULL OR wh.created_at >= :createdFrom)
                        AND (:createdTo IS NULL OR wh.created_at <= :createdTo)
                        AND (:updatedBy IS NULL OR wh.updated_by = :updatedBy)
                        ORDER BY wh.created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findAllWithFilters(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("type") String type,
                        @Param("productName") String productName,
                        @Param("minQuantity") Integer minQuantity,
                        @Param("maxQuantity") Integer maxQuantity,
                        @Param("createdFrom") OffsetDateTime createdFrom,
                        @Param("createdTo") OffsetDateTime createdTo,
                        @Param("updatedBy") String updatedBy,
                        Pageable pageable);

        // ==================== FIND ALL SIMPLE ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findAll(Pageable pageable);

        // ==================== FIND BY WAREHOUSE ID ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findByWarehouseId(
                        @Param("warehouseId") UUID warehouseId,
                        Pageable pageable);

        // ==================== FIND ALL BY WAREHOUSE ID (no paging)
        // ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        ORDER BY created_at DESC
                        """)
        Flux<WarehouseHistory> findAllByWarehouseId(@Param("warehouseId") UUID warehouseId);

        // ==================== FIND BY TYPE ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE type = CAST(:type AS TEXT)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findByType(
                        @Param("type") String type,
                        Pageable pageable);

        // ==================== FIND BY WAREHOUSE ID AND TYPE ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND type = CAST(:type AS TEXT)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findByWarehouseIdAndType(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("type") String type,
                        Pageable pageable);

        // ==================== FIND IMPORTS ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE type = 'import'
                        AND (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                        AND (:createdFrom IS NULL OR created_at >= :createdFrom)
                        AND (:createdTo IS NULL OR created_at <= :createdTo)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findImports(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("createdFrom") OffsetDateTime createdFrom,
                        @Param("createdTo") OffsetDateTime createdTo,
                        Pageable pageable);

        // ==================== FIND EXPORTS ====================
        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE type = 'export'
                        AND (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                        AND (:createdFrom IS NULL OR created_at >= :createdFrom)
                        AND (:createdTo IS NULL OR created_at <= :createdTo)
                        ORDER BY created_at DESC
                        LIMIT :#{#pageable.pageSize}
                        OFFSET :#{#pageable.offset}
                        """)
        Flux<WarehouseHistory> findExports(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("createdFrom") OffsetDateTime createdFrom,
                        @Param("createdTo") OffsetDateTime createdTo,
                        Pageable pageable);

        // ==================== STATISTICS ====================

        @Query("""
                        SELECT COALESCE(SUM(quantity), 0)
                        FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND type = 'import'
                        """)
        Mono<Long> getTotalImportQuantity(@Param("warehouseId") UUID warehouseId);

        @Query("""
                        SELECT COALESCE(SUM(quantity), 0)
                        FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND type = 'export'
                        """)
        Mono<Long> getTotalExportQuantity(@Param("warehouseId") UUID warehouseId);

        @Query("""
                        SELECT COALESCE(SUM(quantity), 0)
                        FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND type = CAST(:type AS TEXT)
                        AND created_at >= :from
                        AND created_at <= :to
                        """)
        Mono<Long> getQuantityByTypeAndDateRange(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("type") String type,
                        @Param("from") OffsetDateTime from,
                        @Param("to") OffsetDateTime to);

        // ==================== COUNT ====================

        @Query("SELECT COUNT(*) FROM warehouse_history")
        Mono<Long> countAll();

        @Query("""
                        SELECT COUNT(*)
                        FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        """)
        Mono<Long> countByWarehouseId(@Param("warehouseId") UUID warehouseId);

        @Query("""
                        SELECT COUNT(*)
                        FROM warehouse_history
                        WHERE type = CAST(:type AS TEXT)
                        """)
        Mono<Long> countByType(@Param("type") String type);

        @Query("""
                        SELECT COUNT(*)
                        FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND type = CAST(:type AS TEXT)
                        """)
        Mono<Long> countByWarehouseIdAndType(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("type") String type);

        // ==================== RECENT ACTIVITIES ====================

        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE created_at >= :since
                        ORDER BY created_at DESC
                        """)
        Flux<WarehouseHistory> findRecentActivities(
                        @Param("since") OffsetDateTime since,
                        Pageable pageable);

        @Query("""
                        SELECT * FROM warehouse_history
                        WHERE warehouse_id = :warehouseId
                        AND created_at >= :since
                        ORDER BY created_at DESC
                        """)
        Flux<WarehouseHistory> findRecentActivitiesByWarehouse(
                        @Param("warehouseId") UUID warehouseId,
                        @Param("since") OffsetDateTime since);
}
