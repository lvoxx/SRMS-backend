package io.github.lvoxx.srms.gateway.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;

public enum RolePermission {
    STAFF(
            "STAFF",
            List.of(
                    new PathsPermission(Paths.CUSTOMERS, Set.of(HttpMethod.GET, HttpMethod.POST)),
                    new PathsPermission(Paths.CONTACTOR, Set.of(HttpMethod.GET, HttpMethod.POST)))),
    MANAGER(
            "MANAGER",
            List.of(
                    new PathsPermission(Paths.CUSTOMERS, Paths.CUSTOMERS.getMethods()),
                    new PathsPermission(Paths.CONTACTOR, Paths.CONTACTOR.getMethods()))),
    ADMIN(
            "ADMIN",
            List.of(
                    new PathsPermission(Paths.CUSTOMERS, Paths.CUSTOMERS.getMethods()),
                    new PathsPermission(Paths.CONTACTOR, Paths.CONTACTOR.getMethods())));

    private final String role;
    private final List<PathsPermission> pathPermissions;

    RolePermission(String role, List<PathsPermission> pathPermissions) {
        this.role = role;
        this.pathPermissions = pathPermissions;
    }

    public String getRole() {
        return role;
    }

    public List<PathsPermission> getPathsPermissions() {
        return Collections.unmodifiableList(pathPermissions);
    }

    // Lớp nội bộ để lưu thông tin path và phương thức HTTP
    public static class PathsPermission {
        private final Paths path;
        private final Set<HttpMethod> methods;

        public PathsPermission(Paths path, Set<HttpMethod> methods) {
            this.path = path;
            this.methods = Collections.unmodifiableSet(methods);
        }

        public String getPaths() {
            return path.getPattern();
        }

        public Set<HttpMethod> getMethods() {
            return methods;
        }
    }

    // Tìm kiếm RolePermission theo role
    public static RolePermission fromRole(String role) {
        return Arrays.stream(values())
                .filter(rp -> rp.getRole().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + role));
    }

    // Kiểm tra quyền truy cập cho role, path và method
    public static boolean hasPermission(String role, String path, HttpMethod method) {
        RolePermission rolePermission = fromRole(role);
        return rolePermission.getPathsPermissions().stream()
                .anyMatch(pp -> pathMatches(pp.getPaths(), path) && pp.getMethods().contains(method));
    }

    // Kiểm tra path có khớp với pattern
    private static boolean pathMatches(String pattern, String path) {
        return path.startsWith(pattern.replace("/**", ""));
    }
}