package io.github.lvoxx.srms.warehouse.websocket;

import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import io.github.lvoxx.srms.warehouse.services.WarehouseStatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseWebSocketHandler implements WebSocketHandler {

    private final WarehouseStatisticService statisticService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        log.info("WebSocket connection established: {} - {}", session.getId(), path);

        Flux<String> messageFlux;

        if (path.contains("/dashboard")) {
            messageFlux = handleDashboardStream();
        } else if (path.contains("/alerts")) {
            messageFlux = handleAlertsStream();
        } else if (path.contains("/warehouse/")) {
            UUID warehouseId = extractWarehouseId(path);
            messageFlux = handleWarehouseDetailsStream(warehouseId);
        } else {
            messageFlux = Flux.just("{\"error\":\"Unknown endpoint\"}");
        }

        return session.send(
            messageFlux
                .map(json -> session.textMessage(json))
                .onErrorResume(e -> {
                    log.error("WebSocket error for session {}: {}", 
                        session.getId(), e.getMessage(), e);
                    return Flux.just(session.textMessage(
                        "{\"error\":\"" + e.getMessage() + "\"}"));
                })
        )
        .doOnTerminate(() -> 
            log.info("WebSocket connection closed: {}", session.getId()))
        .doOnError(e -> 
            log.error("WebSocket connection error: {}", e.getMessage(), e));
    }

    private Flux<String> handleDashboardStream() {
        log.debug("Starting dashboard statistics stream");
        
        return statisticService.streamDashboardStatistics()
            .map(stats -> createWebSocketEvent("DASHBOARD_UPDATE", stats))
            .map(this::toJson)
            .onErrorResume(e -> {
                log.error("Error in dashboard stream: {}", e.getMessage(), e);
                return Flux.just("{\"error\":\"" + e.getMessage() + "\"}");
            });
    }

    private Flux<String> handleAlertsStream() {
        log.debug("Starting alerts stream");
        
        return statisticService.streamWarehouseAlerts()
            .map(alerts -> createWebSocketEvent("ALERT_UPDATE", alerts))
            .map(this::toJson)
            .onErrorResume(e -> {
                log.error("Error in alerts stream: {}", e.getMessage(), e);
                return Flux.just("{\"error\":\"" + e.getMessage() + "\"}");
            });
    }

    private Flux<String> handleWarehouseDetailsStream(UUID warehouseId) {
        log.debug("Starting warehouse details stream for: {}", warehouseId);
        
        if (warehouseId == null) {
            return Flux.just("{\"error\":\"Invalid warehouse ID\"}");
        }
        
        return statisticService.streamWarehouseDetails(warehouseId)
            .map(details -> createWebSocketEvent("WAREHOUSE_UPDATE", details))
            .map(this::toJson)
            .onErrorResume(e -> {
                log.error("Error in warehouse details stream: {}", e.getMessage(), e);
                return Flux.just("{\"error\":\"" + e.getMessage() + "\"}");
            });
    }

    private UUID extractWarehouseId(String path) {
        try {
            String[] parts = path.split("/");
            return UUID.fromString(parts[parts.length - 1]);
        } catch (Exception e) {
            log.error("Failed to extract warehouse ID from path: {}", path, e);
            return null;
        }
    }

    private WarehouseStatisticDTO.WebSocketEvent createWebSocketEvent(
            String eventType, Object data) {
        return WarehouseStatisticDTO.WebSocketEvent.builder()
            .eventType(eventType)
            .data(data)
            .timestamp(java.time.OffsetDateTime.now())
            .build();
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage(), e);
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}