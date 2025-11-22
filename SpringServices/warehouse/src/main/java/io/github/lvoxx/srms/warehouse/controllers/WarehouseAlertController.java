package io.github.lvoxx.srms.warehouse.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.warehouse.config.WarehouseAlertConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for warehouse alert operations.
 * <p>
 * Provides endpoints for manual alert triggering and configuration monitoring.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("warehouse/alerts")
@RequiredArgsConstructor
public class WarehouseAlertController {
    private final WarehouseAlertConfig alertConfiguration;

    /**
     * Updates alert enabled status.
     * <p>
     * Allows enabling or disabling alert publishing at runtime.
     * <p>
     * Example: PUT /api/v1/warehouse/alerts/enabled?status=true
     * 
     * @param enabled true to enable, false to disable
     * @return success response
     */
    @PutMapping("/enabled")
    public ResponseEntity<Map<String, Object>> updateAlertStatus(@RequestParam boolean enabled) {
        log.info("Alert status update requested: {}", enabled);

        alertConfiguration.setEnabled(enabled);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Alert status updated successfully",
                "enabled", enabled,
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * Health check endpoint for alert service.
     * <p>
     * Example: GET /api/v1/warehouse/alerts/health
     * 
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "warehouse-alert-service",
                "alertingEnabled", alertConfiguration.isEnabled(),
                "checkInterval", alertConfiguration.getCheckInterval().toString(),
                "timestamp", System.currentTimeMillis()));
    }
}
