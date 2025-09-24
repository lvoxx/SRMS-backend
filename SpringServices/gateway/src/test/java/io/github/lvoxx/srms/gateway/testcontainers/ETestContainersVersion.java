package io.github.lvoxx.srms.gateway.testcontainers;

public enum ETestContainersVersion {
    KEYCLOAK("quay.io/keycloak/keycloak:26.3"),
    REDIS("redis:latest"),
    WIREMOCK_SERVER("mockserver/mockserver:5.15.0");

    private String version;

    ETestContainersVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }
}