package io.github.lvoxx.srms.kitchen.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import io.github.lvoxx.srms.kitchen.dto.CostSummary;
import io.github.lvoxx.srms.kitchen.models.KitchenInsideHistory;
import reactor.core.publisher.Flux;

public interface KitchenInsideHistoryRepository extends R2dbcRepository<KitchenInsideHistory, UUID> {

        Flux<KitchenInsideHistory> findByTransactionType(String transactionType);

        Flux<KitchenInsideHistory> findByItemName(String itemName);

        @Query("SELECT * FROM kitchen_inside_history " +
                        "WHERE transaction_date >= :startDate AND transaction_date < :endDate " +
                        "ORDER BY transaction_date DESC")
        Flux<KitchenInsideHistory> findByDateRange(
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        @Query("SELECT * FROM kitchen_inside_history " +
                        "WHERE item_name = :itemName " +
                        "ORDER BY transaction_date DESC")
        Flux<KitchenInsideHistory> findHistoryByItemName(@Param("itemName") String itemName);

        @Query("SELECT * FROM kitchen_inside_history " +
                        "WHERE transaction_type = :transactionType " +
                        "AND transaction_date >= :startDate " +
                        "ORDER BY transaction_date DESC")
        Flux<KitchenInsideHistory> findByTypeAndDateAfter(
                        @Param("transactionType") String transactionType,
                        @Param("startDate") OffsetDateTime startDate);

        @Query("""
                        SELECT
                            item_category,
                            COALESCE(SUM(cost_amount), 0) AS total_cost
                        FROM kitchen_inside_history
                        WHERE transaction_type = :transactionType
                          AND transaction_date >= :startDate
                          AND transaction_date < :endDate
                        GROUP BY item_category
                        """)
        Flux<CostSummary> getCostSummaryByCategory(
          @Param("transactionType") String transactionType,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

}