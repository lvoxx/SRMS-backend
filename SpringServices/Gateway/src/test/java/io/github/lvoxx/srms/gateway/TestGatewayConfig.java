package io.github.lvoxx.srms.gateway;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@TestConfiguration
public class TestGatewayConfig {

    @Bean
    RouterFunction<ServerResponse> fallbackRoutes() {
        return route()
                .GET("/fallback/services/customer",
                        req -> ServerResponse.ok().bodyValue("Customer service unavailable"))
                .GET("/fallback/services/contact",
                        req -> ServerResponse.ok().bodyValue("Contact service unavailable"))
                .build();
    }
}
