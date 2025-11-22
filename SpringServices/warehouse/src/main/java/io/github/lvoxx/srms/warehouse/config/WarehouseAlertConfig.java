package io.github.lvoxx.srms.warehouse.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for warehouse alert scheduling.
 * <p>
 * These properties are loaded from application.yml and can be overridden
 * via environment variables or external configuration sources.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "warehouse.alert")
public class WarehouseAlertConfig {

    /**
     * Interval for checking and sending warehouse alerts.
     * <p>
     * Default: 5 minutes
     */
    private Duration checkInterval = Duration.ofMinutes(5);

    /**
     * Initial delay before starting alert checks.
     * <p>
     * Default: 30 seconds (allows system to fully initialize)
     */
    private Duration initialDelay = Duration.ofSeconds(30);

    /**
     * Page size for fetching alerts in batches.
     * <p>
     * Default: 50 items per page
     */
    private int pageSize = 50;

    /**
     * Enable or disable alert publishing.
     * <p>
     * Useful for maintenance or testing scenarios.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Retry attempts for failed Kafka message sends.
     * <p>
     * Default: 3 attempts
     */
    private int retryAttempts = 3;

    /**
     * Delay between retry attempts.
     * <p>
     * Default: 1 second
     */
    private Duration retryDelay = Duration.ofSeconds(1);

    /**
     * Maximum number of alerts to process in a single run.
     * <p>
     * Prevents overwhelming the system with too many messages at once.
     * Default: 1000
     */
    private int maxAlertsPerRun = 1000;

    /**
     * Timeout for blocking operations.
     * <p>
     * Default: 30 seconds
     */
    private Duration operationTimeout = Duration.ofSeconds(30);
}