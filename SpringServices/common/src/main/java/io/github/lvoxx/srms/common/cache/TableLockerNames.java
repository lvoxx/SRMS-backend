package io.github.lvoxx.srms.common.cache;

/**
 * Centralized table lock key definitions used for distributed locking.
 * <p>
 * Each constant represents a lock namespace for a table/module.
 * Used together with ReactiveRowLockService to form unique lock keys.
 *
 * Example: "lock:warehouse:{id}"
 */
public final class TableLockerNames {

    private TableLockerNames() {
        // Utility class - prevent instantiation
    }

    // ====== Core business modules ======
    public static final String CONTACTOR = "contactor";
    public static final String CUSTOMER = "customer";
    public static final String DASHBOARD = "dashboard";
    public static final String KITCHEN = "kitchen";
    public static final String ORDER = "order";
    public static final String PAYMENT = "payment";
    public static final String REPORTING = "reporting";
    public static final String WAREHOUSE = "warehouse";
    public static final String WAREHOUSE_HISTORY = "warehouse_history";

    // ====== Example usage ======
    // lockService.acquireLock(TableLockerNames.WAREHOUSE, warehouseId);
}