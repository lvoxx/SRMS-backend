package io.github.lvoxx.srms.warehouse.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import io.github.lvoxx.srms.warehouse.websocket.WarehouseWebSocketHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final WarehouseWebSocketHandler warehouseWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();

        // Dashboard statistics endpoint
        map.put("/ws/warehouse/dashboard", warehouseWebSocketHandler);

        // Alerts endpoint
        map.put("/ws/warehouse/alerts", warehouseWebSocketHandler);

        // Warehouse details endpoint (supports dynamic ID)
        map.put("/ws/warehouse/**", warehouseWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);

        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
