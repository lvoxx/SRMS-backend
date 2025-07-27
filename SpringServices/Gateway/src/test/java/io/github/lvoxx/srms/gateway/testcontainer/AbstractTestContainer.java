package io.github.lvoxx.srms.gateway.testcontainer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.mockserver.client.MockServerClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SuppressWarnings("resource")
public abstract class AbstractTestContainer {

        protected static final Network NETWORK = Network.newNetwork();

        protected static final String REALM = "srms";
        protected static final String CLIENT_ID = "api-gateway";

        private static final DockerImageName KEYCLOAK_IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.3.2");
        private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.4");
        private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver:5.15.0");

        @Container
        protected static final GenericContainer<?> keycloakContainer = new GenericContainer<>(KEYCLOAK_IMAGE)
                        .withExposedPorts(8080)
                        .withEnv("KEYCLOAK_ADMIN", "admin")
                        .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                        .withCommand("start-dev")
                        .withNetwork(NETWORK)
                        .withNetworkAliases("keycloak-host")
                        .waitingFor(new HttpWaitStrategy()
                                        .forPort(8080)
                                        .forPath("/realms/master")
                                        .withStartupTimeout(Duration.ofMinutes(3)));

        // Redis Container
        @Container
        protected static final GenericContainer<?> redisContainer = new GenericContainer<>(REDIS_IMAGE)
                        .withExposedPorts(6379)
                        .withNetwork(NETWORK)
                        .withNetworkAliases("redis-host")
                        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

        // Kubernetes API Mock
        @Container
        protected static final MockServerContainer kubeApiMockContainer = new MockServerContainer(
                        MOCKSERVER_IMAGE)
                        .withNetwork(NETWORK)
                        .withNetworkAliases("kubeapi-host");

        // Customer Service Mock
        @Container
        protected static final MockServerContainer customerServiceContainer = new MockServerContainer(
                        MOCKSERVER_IMAGE)
                        .withNetwork(NETWORK)
                        .withNetworkAliases("customer-service-host");

        // ... Other services

        // ... Other services

        @BeforeAll
        static void setupContainers() {
                // Start containers theo thứ tự để đảm bảo phụ thuộc
                startContainerWithRetry(keycloakContainer, 3);
                redisContainer.start();
                kubeApiMockContainer.start();
                customerServiceContainer.start();

                // Đợi Keycloak sẵn sàng trước khi cấu hình
                waitForKeycloakReadiness();

                configureKeycloakRealm();
                configureKubernetesDiscovery();
        }

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
                // Keycloak: Sử dụng địa chỉ và cổng ánh xạ bên ngoài
                String keycloakExternalUrl = "http://" + keycloakContainer.getHost() + ":"
                                + keycloakContainer.getMappedPort(8080);
                registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                                () -> keycloakExternalUrl + "/realms/" + REALM);
                registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                                () -> keycloakExternalUrl + "/realms/" + REALM + "/protocol/openid-connect/certs");

                // Redis: Sử dụng địa chỉ và cổng ánh xạ bên ngoài
                registry.add("spring.redis.host", () -> redisContainer.getHost());
                registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));

                // Kubernetes API Mock: Sử dụng địa chỉ và cổng ánh xạ bên ngoài
                registry.add("spring.cloud.kubernetes.client.kubernetes-api-server",
                                () -> "http://" + kubeApiMockContainer.getHost() + ":"
                                                + kubeApiMockContainer.getServerPort());

                registry.add("spring.cloud.kubernetes.discovery.enabled", () -> "true");
                registry.add("spring.cloud.config.enabled", () -> "false");
        }

        private static void waitForKeycloakReadiness() {
                int attempts = 0;
                int maxAttempts = 10;
                while (attempts < maxAttempts) {
                        try {
                                Keycloak keycloak = KeycloakBuilder.builder()
                                                .serverUrl("http://" + keycloakContainer.getHost() + ":"
                                                                + keycloakContainer.getMappedPort(8080) + "/")
                                                .realm("master")
                                                .username("admin")
                                                .password("admin")
                                                .clientId("admin-cli")
                                                .build();

                                // Kiểm tra kết nối
                                keycloak.realms().findAll();
                                return; // Thành công
                        } catch (Exception e) {
                                attempts++;
                                if (attempts >= maxAttempts) {
                                        throw new RuntimeException(
                                                        "Keycloak not ready after " + maxAttempts + " attempts", e);
                                }
                                try {
                                        TimeUnit.SECONDS.sleep(5);
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                }
                        }
                }
        }

        private static void startContainerWithRetry(GenericContainer<?> container, int maxRetries) {
                int retryCount = 0;
                while (retryCount < maxRetries) {
                        try {
                                container.start();
                                return;
                        } catch (Exception e) {
                                retryCount++;
                                if (retryCount >= maxRetries) {
                                        throw new RuntimeException(
                                                        "Failed to start container after " + maxRetries + " attempts",
                                                        e);
                                }
                                try {
                                        TimeUnit.SECONDS.sleep(5 * retryCount);
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                }
                        }
                }
        }

        private static void configureKeycloakRealm() {
                // Tạo realm với retry
                int attempts = 0;
                int maxAttempts = 5;
                while (attempts < maxAttempts) {
                        try {
                                Keycloak keycloak = KeycloakBuilder.builder()
                                                .serverUrl("http://" + keycloakContainer.getHost() + ":"
                                                                + keycloakContainer.getMappedPort(8080) + "/")
                                                .realm("master")
                                                .username("admin")
                                                .password("admin")
                                                .clientId("admin-cli")
                                                .build();

                                RealmRepresentation realm = new RealmRepresentation();
                                realm.setRealm(REALM);
                                realm.setEnabled(true);
                                keycloak.realms().create(realm);
                                return;
                        } catch (Exception e) {
                                attempts++;
                                if (attempts >= maxAttempts) {
                                        throw new RuntimeException("Failed to create Keycloak realm after "
                                                        + maxAttempts + " attempts", e);
                                }
                                try {
                                        TimeUnit.SECONDS.sleep(5);
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                }
                        }
                }
        }

        private static void configureKubernetesDiscovery() {
                new MockServerClient(kubeApiMockContainer.getHost(), kubeApiMockContainer.getServerPort())
                                .when(org.mockserver.model.HttpRequest.request()
                                                .withPath("/api/v1/namespaces/dev/endpoints/customer-service"))
                                .respond(org.mockserver.model.HttpResponse.response()
                                                .withBody(String.format("""
                                                                {
                                                                    "subsets": [{
                                                                        "addresses": [{"ip": "customer-service-host"}],
                                                                        "ports": [{"port": %d}]
                                                                    }]
                                                                }
                                                                """, customerServiceContainer.getServerPort()))
                                                .withStatusCode(200));
        }

        protected String getAccessToken(String username) {
                Keycloak keycloak = KeycloakBuilder.builder()
                                .serverUrl("http://" + keycloakContainer.getHost() + ":"
                                                + keycloakContainer.getMappedPort(8080) + "/")
                                .realm(REALM)
                                .clientId(CLIENT_ID)
                                .username(username)
                                .password("testpass")
                                .build();
                return keycloak.tokenManager().getAccessTokenString();
        }

        protected MockServerClient getCustomerServiceMock() {
                return new MockServerClient(
                                customerServiceContainer.getHost(),
                                customerServiceContainer.getServerPort());
        }

        protected void resetCustomerServiceMocks() {
                getCustomerServiceMock().reset();
        }
}