package io.github.lvoxx.srms.common.cache;

import lombok.experimental.UtilityClass;

/**
 * Centralized cache name definitions for warehouse-related services.
 * 
 * Use directly in @Cacheable annotations, e.g.:
 * 
 * <pre>
 * {@code
 * @Cacheable(value = WarehouseCacheNames.COUNT_ALL, key = "#includeDeleted")
 * public Mono<Long> countAll(boolean includeDeleted) { ... }
 * }
 * </pre>
 */
@UtilityClass
public class WarehouseCacheNames {

    // ==================== COUNT SERVICE ====================

    public static final String COUNT_ALL = "warehouse:count:all";
    public static final String COUNT_BELOW_MINIMUM = "warehouse:count:below-minimum";
    public static final String COUNT_OUT_OF_STOCK = "warehouse:count:out-of-stock";
    public static final String COUNT_HISTORY_ALL = "warehouse:count:history:all";
    public static final String COUNT_HISTORY_BY_WAREHOUSE = "warehouse:count:history:by-warehouse";
    public static final String COUNT_HISTORY_BY_TYPE = "warehouse:count:history:by-type";
    public static final String COUNT_HISTORY_BY_WAREHOUSE_AND_TYPE = "warehouse:count:history:by-warehouse-and-type";
    public static final String COUNT_STATISTICS = "warehouse:count:statistics";
    public static final String COUNT_HEALTH = "warehouse:count:health";

    // ==================== STATISTIC SERVICE ====================

    public static final String STATS_TOTAL_IMPORT = "warehouse:stats:total-import";
    public static final String STATS_TOTAL_EXPORT = "warehouse:stats:total-export";
    public static final String STATS_QUANTITY_BY_DATE_RANGE = "warehouse:stats:quantity-by-date-range";
    public static final String STATS_BALANCE = "warehouse:stats:balance";
    public static final String STATS_BELOW_MINIMUM = "warehouse:stats:below-minimum";
    public static final String STATS_OUT_OF_STOCK = "warehouse:stats:out-of-stock";
    public static final String STATS_ALL_ALERTS = "warehouse:stats:all-alerts";
    public static final String STATS_DASHBOARD = "warehouse:stats:dashboard";
    public static final String STATS_DETAILS = "warehouse:stats:details";
    public static final String STATS_TIME_BASED = "warehouse:stats:time-based";
}