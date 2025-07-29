package io.github.lvoxx.srms.gateway.testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.Data;
import reactor.core.publisher.Mono;

@Testcontainers
@SuppressWarnings("resource")
public class AbstractTestContainers {

        private static final String REALM_FILE = "realm-srms.json";
        private static final String CLIENT_SECRET = "test-secret";
        private static final String CLIENT_ID = "gateway-client";

        @Container
        protected static final KeycloakContainer keycloak = new KeycloakContainer(
                        ETestContainersVersion.KEYCLOAK.getVersion())
                        .withRealmImportFile(REALM_FILE)
                        .withStartupTimeout(Duration.ofSeconds(360)); // 3 minutes

        @Container
        protected static final GenericContainer<?> redis = new GenericContainer<>(
                        ETestContainersVersion.REDIS.getVersion())
                        .withExposedPorts(6379);

        @Container
        protected static final MockServerContainer customerServiceMock = new MockServerContainer(
                        DockerImageName.parse(ETestContainersVersion.WIREMOCK_SERVER.getVersion()));

        @Container
        protected static final MockServerContainer contactServiceMock = new MockServerContainer(
                        DockerImageName.parse(ETestContainersVersion.WIREMOCK_SERVER.getVersion()));

        protected static WebClient keycloakClient;

        @DynamicPropertySource
        static void registerProperties(DynamicPropertyRegistry registry) {
                registry.add("keycloakIssuerUri", () -> keycloak.getAuthServerUrl() + "/realms/srms");
                registry.add("redisHost", redis::getHost);
                registry.add("redisPort", () -> redis.getMappedPort(6379).toString());
                registry.add("customerServiceUri",
                                () -> "http://" + customerServiceMock.getHost() + ":"
                                                + customerServiceMock.getServerPort());
                registry.add("contactServiceUri",
                                () -> "http://" + contactServiceMock.getHost() + ":"
                                                + contactServiceMock.getServerPort());
        }

        @BeforeAll
        static void setup() {
                keycloak.start();
                redis.start();
        }

        @AfterAll
        static void tearDown() {
                keycloak.stop();
                redis.stop();
        }

        protected String getTokenForUser(String username, String password) {
                // Log request details
                String form = "grant_type=password"
                                + "&client_id=" + CLIENT_ID
                                + "&client_secret=" + CLIENT_SECRET
                                + "&username=" + UriUtils.encode(username, StandardCharsets.UTF_8)
                                + "&password=" + UriUtils.encode(password, StandardCharsets.UTF_8)
                                + "&scope=openid"; // Scope để yêu cầu claims

                System.out.println("🔵 Sending token request to Keycloak:");
                System.out.println("URL: " + keycloakClient + "/realms/srms/protocol/openid-connect/token");
                System.out.println("Form data: " + form.replace(CLIENT_SECRET, "******")); // Ẩn secret trong log

                // Gửi request và log response RAW (dạng string trước khi parse JSON)
                return keycloakClient.post()
                                .uri("/realms/srms/protocol/openid-connect/token")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .bodyValue(form)
                                .retrieve()
                                .bodyToMono(String.class) // Đọc response dạng string trước
                                .doOnNext(rawResponse -> {
                                        System.out.println("🟢 Raw response from Keycloak:");
                                        System.out.println(rawResponse);
                                })
                                .flatMap(rawResponse -> {
                                        try {
                                                // (3) Parse JSON thủ công để bắt lỗi cụ thể
                                                ObjectMapper mapper = new ObjectMapper();
                                                TokenResponse tokenResponse = mapper.readValue(rawResponse,
                                                                TokenResponse.class);

                                                if (tokenResponse.getAccessToken() == null) {
                                                        System.err.println("❌ Token is null in response!");
                                                        return Mono.error(new RuntimeException(
                                                                        "Token is null in response. Raw response: "
                                                                                        + rawResponse));
                                                }
                                                return Mono.just(tokenResponse.getAccessToken());
                                        } catch (JsonProcessingException e) {
                                                System.err.println(
                                                                "❌ Failed to parse JSON response: " + e.getMessage());
                                                return Mono.error(new RuntimeException(
                                                                "JSON parse error: " + rawResponse, e));
                                        }
                                })
                                .onErrorResume(e -> {
                                        System.err.println("❌ Final error: " + e.getMessage());
                                        return Mono.error(
                                                        new RuntimeException("Failed to get token for " + username, e));
                                })
                                .block();
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        protected static class TokenResponse {
                @JsonProperty("access_token")
                @JsonAlias({ "access_token", "accessToken" })
                private String accessToken;
        }
}
