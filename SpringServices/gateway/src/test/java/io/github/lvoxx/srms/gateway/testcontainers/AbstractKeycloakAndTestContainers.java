package io.github.lvoxx.srms.gateway.testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.Data;
import reactor.core.publisher.Mono;

@Testcontainers
@SuppressWarnings({ "resource" })
public abstract class AbstractKeycloakAndTestContainers extends AbstractTestContainers {

    private static final Logger log = LoggerFactory.getLogger(AbstractKeycloakAndTestContainers.class);

    private static final String CLIENT_SECRET = "test-secret";
    private static final String CLIENT_ID = "gateway-client";
    private static final String REALM_FILE = "realm-srms.json";

    @Container
    protected static final KeycloakContainer keycloak = new KeycloakContainer(
            ETestContainersVersion.KEYCLOAK.getVersion())
            .withRealmImportFile(REALM_FILE)
            .withStartupTimeout(Duration.ofSeconds(120)); // 2 minutes

    protected static WebClient keycloakClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloakIssuerUri", () -> keycloak.getAuthServerUrl() + "/realms/srms");
    }

    @BeforeAll
    static void setup() {
        keycloak.start();
    }

    @AfterAll
    static void tearDown() {
        keycloak.stop();
    }

    protected static String getTokenForUser(String username, String password) {
        // Log request details
        String form = "grant_type=password"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&username=" + UriUtils.encode(username, StandardCharsets.UTF_8)
                + "&password=" + UriUtils.encode(password, StandardCharsets.UTF_8)
                + "&scope=openid"; // Scope để yêu cầu claims

        log.info("🔵 Sending token request to Keycloak:");
        log.info("URL: " + keycloakClient + "/realms/srms/protocol/openid-connect/token");
        log.info("Form data: " + form.replace(CLIENT_SECRET, "******")); // Ẩn secret trong log

        // Gửi request và log response RAW (dạng string trước khi parse JSON)
        return keycloakClient.post()
                .uri("/realms/srms/protocol/openid-connect/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(String.class) // Đọc response dạng string trước
                .doOnNext(rawResponse -> {
                    log.info("🟢 Got JWT token from Keycloak");
                    // log.info(rawResponse);
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