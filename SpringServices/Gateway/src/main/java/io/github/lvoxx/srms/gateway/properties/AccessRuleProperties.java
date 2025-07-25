package io.github.lvoxx.srms.gateway.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.access-rules")
public class AccessRuleProperties {

    private Map<String, List<String>> accessRules = new HashMap<>();

    public void setAccessRules(Map<String, List<String>> accessRules) {
        this.accessRules = accessRules;
    }

    public List<String> getPathsForRole(String role) {
        Set<String> result = new HashSet<>();
        switch (role.toUpperCase()) {
            // For public access
            case "PUBLIC" -> result.addAll(accessRules.getOrDefault("public", List.of()));
            // Apply role inheritance (STAFF < MANAGER < ADMIN)
            case "STAFF" -> result.addAll(accessRules.getOrDefault("staff", List.of()));
            case "MANAGER" -> {
                result.addAll(accessRules.getOrDefault("staff", List.of()));
                result.addAll(accessRules.getOrDefault("manager", List.of()));
            }
            case "ADMIN" -> {
                result.addAll(accessRules.getOrDefault("staff", List.of()));
                result.addAll(accessRules.getOrDefault("manager", List.of()));
                result.addAll(accessRules.getOrDefault("admin", List.of()));
            }
        }
        return new ArrayList<>(result);
    }
}
