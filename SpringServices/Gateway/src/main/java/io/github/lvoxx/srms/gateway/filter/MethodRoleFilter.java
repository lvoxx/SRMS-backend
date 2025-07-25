package io.github.lvoxx.srms.gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class MethodRoleFilter extends AbstractGatewayFilterFactory<MethodRoleFilter.Config> {

    public MethodRoleFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().toString();

            if (config.getMethods().contains(method) &&
                    config.getPaths().stream().anyMatch(path::contains)) {

                return exchange.getPrincipal()
                        .flatMap(principal -> {
                            if (principal instanceof JwtAuthenticationToken) {
                                JwtAuthenticationToken jwt = (JwtAuthenticationToken) principal;
                                boolean hasBlockedRole = jwt.getAuthorities().stream()
                                        .anyMatch(auth -> config.getBlockedRoles().contains(auth.getAuthority()));

                                if (hasBlockedRole) {
                                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                    return exchange.getResponse().setComplete();
                                }
                            }
                            return chain.filter(exchange);
                        });
            }
            return chain.filter(exchange);
        };
    }

    @Data
    public static class Config {
        private List<String> methods;
        private List<String> paths;
        private List<String> blockedRoles;
        private String fallbackUri;
    }
}