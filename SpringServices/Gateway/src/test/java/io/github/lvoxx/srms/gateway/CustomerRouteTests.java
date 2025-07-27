package io.github.lvoxx.srms.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.github.lvoxx.srms.gateway.testcontainer.AbstractTestContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                // Sử dụng internal network cho tất cả kết nối
                "spring.cloud.kubernetes.discovery.use-endpoint-slices=false",
                "spring.cloud.kubernetes.discovery.port-name=http"
})
@SuppressWarnings("resource")
public class CustomerRouteTests extends AbstractTestContainer {

        @LocalServerPort
        private int gatewayPort;

        @Autowired
        private WebTestClient webTestClient;

        @BeforeEach
        void setup() {
                // Reset mocks trước mỗi test
                new MockServerClient(
                                customerServiceContainer.getHost(),
                                customerServiceContainer.getServerPort()).reset();
        }

        @Test
        void routeToCustomerService_shouldRewritePathAndAddHeaders() {
                // Configure specific mock response
                getCustomerServiceMock()
                                .when(request().withPath("/customers/123"))
                                .respond(response().withStatusCode(200).withBody("Customer 123 Details"));

                String token = getAccessToken("staff_user");

                webTestClient.get().uri("/api/customers/123")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().exists("X-Request-ID")
                                .expectBody(String.class).isEqualTo("Customer 123 Details");

                // Verify request to customer service
                HttpRequest[] requests = getCustomerServiceMock()
                                .retrieveRecordedRequests(request().withPath("/customers/123"));

                assertThat(requests).hasSize(1);
                assertThat(requests[0].getHeader("Authorization")).isNotNull();
                assertThat(requests[0].getHeader("X-Request-ID")).isNotNull();
        }

        @Test
        void stripPrefixFilter_shouldRemoveApiPrefix() {
                // Configure mock to verify exact path
                getCustomerServiceMock()
                                .when(request().withPath("/customers/456"))
                                .respond(response().withBody("Strip prefix works"));

                String token = getAccessToken("staff_user");

                webTestClient.get().uri("/api/customers/456")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(String.class).isEqualTo("Strip prefix works");
        }

        @Test
        void methodRoleFilter_shouldBlockDeleteForStaff() {
                String token = getAccessToken("staff_user");

                webTestClient.delete().uri("/api/customers/789")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isForbidden()
                                .expectBody()
                                .jsonPath("$.message").isEqualTo("Access Denied");
        }

        @Test
        void rateLimiter_shouldBlockAfterThreshold() {
                String token = getAccessToken("staff_user");

                // First 10 requests should succeed
                for (int i = 0; i < 10; i++) {
                        webTestClient.get().uri("/api/customers")
                                        .header("Authorization", "Bearer " + token)
                                        .exchange()
                                        .expectStatus().isOk();
                }

                // Next request should be rate limited
                webTestClient.get().uri("/api/customers")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isEqualTo(429)
                                .expectBody()
                                .jsonPath("$.error").isEqualTo("Too Many Requests");
        }

        @Test
        void circuitBreaker_shouldTriggerFallbackAfterFailures() {
                // Configure customer service to fail
                getCustomerServiceMock()
                                .when(request().withPath("/customers/failing"))
                                .respond(response().withStatusCode(502));

                String token = getAccessToken("staff_user");

                // First call - should fail after retries
                webTestClient.get().uri("/api/customers/failing")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isEqualTo(502);

                // Subsequent calls should trigger circuit breaker fallback
                webTestClient.get().uri("/api/customers/failing")
                                .header("Authorization", "Bearer " + token)
                                .exchange()
                                .expectStatus().isEqualTo(503)
                                .expectBody()
                                .jsonPath("$.message").isEqualTo("Service Unavailable");
        }

        @Test
        void publicEndpoints_shouldNotRequireAuth() {
                webTestClient.get().uri("/api/actuator/health")
                                .exchange()
                                .expectStatus().isOk();
        }
}
