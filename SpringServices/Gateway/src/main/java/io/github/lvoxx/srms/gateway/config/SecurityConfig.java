package io.github.lvoxx.srms.gateway.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import io.github.lvoxx.srms.gateway.rules.RolePermission;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        // Setup security
        http
                .authorizeExchange(auth -> {
                    auth.pathMatchers("/fallback/**").permitAll();
                    auth.pathMatchers("/actuator/**").permitAll();
                    auth.pathMatchers("/health/**").permitAll();
                    auth.pathMatchers("/info/**").permitAll();
                    auth.pathMatchers("/metrics/**").permitAll();
                    auth.pathMatchers("/trace/**").permitAll();

                    // Cấu hình quyền động từ RolePermission
                    for (RolePermission rolePermission : RolePermission.values()) {
                        for (RolePermission.PathsPermission pathPermission : rolePermission.getPathsPermissions()) {
                            String path = pathPermission.getPaths();
                            for (org.springframework.http.HttpMethod method : pathPermission.getMethods()) {
                                auth.pathMatchers(method, path).hasRole(rolePermission.getRole());
                            }
                        }
                    }
                    auth.anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()); // Disable CSRF for using JWT
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Check for gateway-<profile>.yml configuration on repo
        config.setAllowedOrigins(List.of("${security.cors.allowed-origins}"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true); // Important if using cookies / sessions

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @SuppressWarnings("unchecked")
    ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            // ROLE_ + ADMIN -> ROLE_ADMIN
                            "ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
