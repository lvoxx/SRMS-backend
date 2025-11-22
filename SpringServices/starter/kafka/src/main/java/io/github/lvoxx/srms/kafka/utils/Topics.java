package io.github.lvoxx.srms.kafka.utils;

/**
 * Central definition of all Kafka topics used across the SRMS system.
 * <p>
 * This abstract class serves as a single source of truth for topic names,
 * making it easier to maintain consistency and manage topic changes across
 * modules.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
public abstract class Topics {

    // ==================== WAREHOUSE TOPICS ====================

    /**
     * Topic for warehouse alert messages.
     * <p>
     * Contains alerts for products that are below minimum quantity (WARNING)
     * or completely out of stock (CRITICAL).
     * <p>
     * Message Type:
     * {@link io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage}
     */
    public static final String WAREHOUSE_ALERTS = "srms-warehouse-alerts";

    /**
     * Topic for warehouse inventory updates.
     * <p>
     * Contains real-time inventory change events (imports/exports).
     */
    public static final String WAREHOUSE_INVENTORY_UPDATES = "srms-warehouse-inventory-updates";

    /**
     * Topic for warehouse statistics snapshots.
     * <p>
     * Contains periodic snapshots of warehouse statistics for analytics.
     */
    public static final String WAREHOUSE_STATISTICS = "srms-warehouse-statistics";

    // ==================== CONFIGURATION TOPICS ====================

    /**
     * Topic for system-wide configuration updates.
     * <p>
     * Contains configuration changes that services should react to in real-time.
     */
    public static final String CONFIGURATION = "srms-configuration";

    // ==================== NOTIFICATION TOPICS ====================

    /**
     * Topic for notification messages.
     * <p>
     * Contains various notification events to be sent to users.
     */
    public static final String NOTIFICATIONS = "srms-notifications";

    // ==================== TOPIC GROUPS ====================

    /**
     * Consumer group ID for warehouse alert processors.
     */
    public static final String WAREHOUSE_ALERT_CONSUMER_GROUP = "warehouse-alert-consumer-group";

    /**
     * Consumer group ID for notification services.
     */
    public static final String NOTIFICATION_CONSUMER_GROUP = "notification-consumer-group";

    /**
     * Consumer group ID for analytics services.
     */
    public static final String ANALYTICS_CONSUMER_GROUP = "analytics-consumer-group";

    // Prevent instantiation
    private Topics() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}