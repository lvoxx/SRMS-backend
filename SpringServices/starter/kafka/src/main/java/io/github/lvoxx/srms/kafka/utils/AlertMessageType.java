package io.github.lvoxx.srms.kafka.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the types and severity of warehouse alert messages.
 * <p>
 * This enum provides a structured way to categorize alerts based on
 * inventory levels and business rules.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AlertMessageType {

    /**
     * WARNING level alert.
     * <p>
     * Triggered when product quantity falls below minimum threshold
     * but is still available in stock (quantity > 0).
     * <p>
     * Action Required: Plan restocking, create purchase orders.
     */
    BELOW_MINIMUM(
            "WARNING",
            "Product quantity below minimum threshold",
            "Restocking recommended - Please create purchase order"),

    /**
     * CRITICAL level alert.
     * <p>
     * Triggered when product is completely out of stock (quantity = 0).
     * <p>
     * Action Required: Immediate restocking, notify sales team,
     * consider backorder management.
     */
    OUT_OF_STOCK(
            "CRITICAL",
            "Product is out of stock",
            "Immediate restocking required - Notify sales and procurement teams");

    /**
     * Severity level matching Avro AlertLevel enum (WARNING, CRITICAL)
     */
    private final String severity;

    /**
     * Short description of the alert condition
     */
    private final String description;

    /**
     * Recommended action for this alert type
     */
    private final String recommendedAction;

    /**
     * Checks if this alert type is critical.
     * 
     * @return true if alert is CRITICAL level
     */
    public boolean isCritical() {
        return this == OUT_OF_STOCK;
    }

    /**
     * Gets the Avro AlertLevel corresponding to this message type.
     * 
     * @return AlertLevel enum value
     */
    public io.github.lvoxx.srms.kafka.warehouse.AlertLevel toAvroAlertLevel() {
        return switch (this) {
            case BELOW_MINIMUM -> io.github.lvoxx.srms.kafka.warehouse.AlertLevel.WARNING;
            case OUT_OF_STOCK -> io.github.lvoxx.srms.kafka.warehouse.AlertLevel.CRITICAL;
        };
    }

    /**
     * Creates an AlertMessageType from severity string.
     * 
     * @param severity severity level ("WARNING" or "CRITICAL")
     * @return corresponding AlertMessageType
     * @throws IllegalArgumentException if severity is not recognized
     */
    public static AlertMessageType fromSeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "WARNING" -> BELOW_MINIMUM;
            case "CRITICAL" -> OUT_OF_STOCK;
            default -> throw new IllegalArgumentException("Unknown severity: " + severity);
        };
    }

    /**
     * Creates an AlertMessageType from AlertLevel enum.
     * 
     * @param alertLevel Avro AlertLevel enum
     * @return corresponding AlertMessageType
     */
    public static AlertMessageType fromAlertLevel(io.github.lvoxx.srms.kafka.warehouse.AlertLevel alertLevel) {
        return switch (alertLevel) {
            case WARNING -> BELOW_MINIMUM;
            case CRITICAL -> OUT_OF_STOCK;
        };
    }
}