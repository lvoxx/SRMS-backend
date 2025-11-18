package io.github.lvoxx.srms.warehouse.services;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.lvoxx.srms.kafka.utils.AlertMessageType;
import io.github.lvoxx.srms.kafka.utils.Topics;
import io.github.lvoxx.srms.kafka.warehouse.AlertLevel;
import io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage;
import io.github.lvoxx.srms.warehouse.config.WarehouseAlertConfig;
import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for producing warehouse alert messages to Kafka.
 * <p>
 * Periodically scans warehouse inventory for products that are below minimum
 * quantity or out of stock, then publishes alert messages to Kafka for
 * downstream processing and notifications.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseAlertProducerService {

    private final KafkaTemplate<String, WarehouseAlertMessage> kafkaTemplate;
    private final WarehouseStatisticService statisticService;
    private final WarehouseAlertConfig alertConfig;

    // ==================== SCHEDULED ALERT PUBLISHING ====================

    /**
     * Scheduled task to check and publish warehouse alerts.
     * <p>
     * Runs at fixed rate based on configuration (default: every 5 minutes).
     * Fetches all alerts (below minimum + out of stock) and publishes them to
     * Kafka.
     * <p>
     * The schedule can be dynamically updated via configuration topic.
     */
    @Scheduled(fixedDelayString = "#{@warehouseAlertConfiguration.checkInterval.toMillis()}", initialDelayString = "#{@warehouseAlertConfiguration.initialDelay.toMillis()}")
    public void publishWarehouseAlerts() {
        if (!alertConfig.isEnabled()) {
            log.debug("Warehouse alert publishing is disabled");
            return;
        }

        log.info("Starting scheduled warehouse alert check");

        int page = 0;
        long totalAlertsSent = 0;
        long totalAlertsProcessed = 0;

        try {
            // Fetch alerts in pages to handle large datasets
            WarehouseStatisticDTO.AlertListResponse response = statisticService
                    .getAllWarehouseAlerts(page, alertConfig.getPageSize())
                    .block();

            if (response == null || response.getItems().isEmpty()) {
                log.info("No warehouse alerts found");
                return;
            }

            totalAlertsProcessed = response.getTotalItems();
            log.info("Found {} total alerts to process", totalAlertsProcessed);

            // Process all pages
            do {
                for (WarehouseStatisticDTO.AlertItem alert : response.getItems()) {
                    try {
                        publishAlertMessage(alert)
                                .thenAccept(result -> {
                                    log.debug("Successfully sent alert for product {} to partition {}",
                                            alert.getProductName(),
                                            result.getRecordMetadata().partition());
                                })
                                .exceptionally(ex -> {
                                    log.error("Failed to send alert for product {}: {}",
                                            alert.getProductName(), ex.getMessage());
                                    return null;
                                });

                        totalAlertsSent++;
                    } catch (Exception e) {
                        log.error("Error processing alert for product {}: {}",
                                alert.getProductName(), e.getMessage(), e);
                    }
                }

                // Fetch next page if available
                page++;
                if (page * alertConfig.getPageSize() < totalAlertsProcessed) {
                    response = statisticService.getAllWarehouseAlerts(page, alertConfig.getPageSize())
                            .block();
                } else {
                    break;
                }

            } while (response != null && !response.getItems().isEmpty());

            log.info("Warehouse alert check completed. Sent {}/{} alerts",
                    totalAlertsSent, totalAlertsProcessed);

        } catch (Exception e) {
            log.error("Error during scheduled warehouse alert check: {}", e.getMessage(), e);
        }
    }

    // ==================== MESSAGE PUBLISHING ====================

    /**
     * Publishes a single alert message to Kafka.
     * <p>
     * Converts AlertItem to WarehouseAlertMessage and sends it to the
     * warehouse-alerts topic with retry logic.
     * 
     * @param alert the alert item to publish
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, WarehouseAlertMessage>> publishAlertMessage(
            WarehouseStatisticDTO.AlertItem alert) {

        WarehouseAlertMessage message = buildAlertMessage(alert);

        log.debug("Publishing alert message for product: {} (severity: {})",
                alert.getProductName(), alert.getSeverity());

        return kafkaTemplate.send(
                Topics.WAREHOUSE_ALERTS,
                alert.getId().toString(),
                message);
    }

    /**
     * Publishes an alert message reactively.
     * 
     * @param alert the alert item to publish
     * @return Mono completing when message is sent
     */
    public Mono<Void> publishAlertMessageReactive(WarehouseStatisticDTO.AlertItem alert) {
        return Mono.fromFuture(publishAlertMessage(alert))
                .doOnSuccess(
                        result -> log.debug("Alert published successfully for product: {}", alert.getProductName()))
                .doOnError(error -> log.error("Failed to publish alert for product: {}", alert.getProductName(), error))
                .then();
    }

    /**
     * Publishes multiple alert messages in batch.
     * 
     * @param alertList the list of alerts to publish
     * @return Mono completing when all messages are sent
     */
    public Mono<Void> publishAlertBatch(WarehouseStatisticDTO.AlertListResponse alertList) {
        log.info("Publishing batch of {} alerts", alertList.getItems().size());

        return Mono.when(
                alertList.getItems().stream()
                        .map(this::publishAlertMessageReactive)
                        .toList())
                .doOnSuccess(v -> log.info("Successfully published batch of {} alerts", alertList.getItems().size()))
                .doOnError(error -> log.error("Error publishing alert batch: {}", error.getMessage(), error));
    }

    // ==================== MESSAGE BUILDING ====================

    /**
     * Builds a WarehouseAlertMessage from an AlertItem.
     * <p>
     * Determines alert level based on severity:
     * - CRITICAL -> Out of stock (quantity = 0)
     * - WARNING -> Below minimum threshold
     * 
     * @param alert the alert item
     * @return constructed WarehouseAlertMessage
     */
    private WarehouseAlertMessage buildAlertMessage(WarehouseStatisticDTO.AlertItem alert) {
        AlertLevel level = determineAlertLevel(alert);
        AlertMessageType messageType = level == AlertLevel.CRITICAL
                ? AlertMessageType.OUT_OF_STOCK
                : AlertMessageType.BELOW_MINIMUM;

        String enhancedMessage = String.format(
                "[%s]%s - Current: %d, Minimum: %d, Deficit: %d units.\n %s",
                messageType.getSeverity(),
                messageType.getDescription(),
                alert.getMessage(),
                alert.getCurrentQuantity(),
                alert.getMinQuantity(),
                alert.getDeficit(),
                messageType.getRecommendedAction());

        return WarehouseAlertMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setProductId(alert.getId().toString())
                .setProductName(alert.getProductName())
                .setCurrentQuantity(alert.getCurrentQuantity())
                .setThreshold(alert.getMinQuantity())
                .setLevel(level)
                .setMessage(enhancedMessage)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    /**
     * Determines AlertLevel based on alert severity.
     * 
     * @param alert the alert item
     * @return AlertLevel enum value
     */
    private AlertLevel determineAlertLevel(WarehouseStatisticDTO.AlertItem alert) {
        return switch (alert.getSeverity()) {
            case "CRITICAL" -> AlertLevel.CRITICAL;
            case "WARNING" -> AlertLevel.WARNING;
            default -> {
                log.warn("Unknown severity '{}' for product {}, defaulting to WARNING",
                        alert.getSeverity(), alert.getProductName());
                yield AlertLevel.WARNING;
            }
        };
    }

    // ==================== MANUAL TRIGGER ====================

    /**
     * Manually triggers an alert check and publish cycle.
     * <p>
     * Useful for testing or immediate alert generation.
     * 
     * @return Mono completing when alerts are published
     */
    public Mono<Long> triggerAlertCheck() {
        log.info("Manual alert check triggered");

        return statisticService.getAllWarehouseAlerts(0, alertConfig.getPageSize())
                .flatMap(this::publishAlertBatch)
                .then(statisticService.getAllWarehouseAlerts(0, alertConfig.getPageSize()))
                .map(response -> (long) response.getItems().size())
                .doOnSuccess(count -> log.info("Manual alert check completed. Sent {} alerts", count))
                .doOnError(error -> log.error("Error during manual alert check: {}", error.getMessage(), error));
    }
}