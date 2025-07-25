package io.github.lvoxx.srms.gateway.config;

import java.util.List;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import io.github.lvoxx.srms.gateway.properties.AccessRuleProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.profiles.active:docker}") // Default to 'docker' if not set
    private String activedProfile;

    private final AccessRuleProperties accessRuleProps;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // CRSF Configuration
        if (isDev()) {
            // For Chill Dev
            http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        } else {
            // For Production
            WebSessionServerCsrfTokenRepository csrfTokenRepository = new WebSessionServerCsrfTokenRepository();
            csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");

            http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository));
        }

        // Access Rules Configuration
        List<String> publicPaths = accessRuleProps.getPathsForRole("PUBLIC");
        List<String> staffPaths = accessRuleProps.getPathsForRole("STAFF");
        List<String> managerPaths = accessRuleProps.getPathsForRole("MANAGER");
        List<String> adminPaths = accessRuleProps.getPathsForRole("ADMIN");

        // Setup security
        http
                .authorizeExchange(auth -> auth
                        .pathMatchers(publicPaths.toArray(new String[0])).permitAll()
                        .pathMatchers(staffPaths.toArray(new String[0])).hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .pathMatchers(managerPaths.toArray(new String[0])).hasAnyRole("MANAGER", "ADMIN")
                        .pathMatchers(adminPaths.toArray(new String[0])).hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwkSetUri("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    KeycloakSpringBootConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
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

    private boolean isDev() {
        return "docker".equalsIgnoreCase(activedProfile);
    }
}
