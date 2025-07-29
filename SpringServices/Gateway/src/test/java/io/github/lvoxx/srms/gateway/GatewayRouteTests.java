package io.github.lvoxx.srms.gateway;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.lvoxx.srms.gateway.testcontainers.AbstractTestContainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestGatewayConfig.class)
public class GatewayRouteTests extends AbstractTestContainers {
    @Autowired
    private WebTestClient webTestClient;

    private static MockServerClient customerClient;
    private static MockServerClient contactClient;

    @BeforeEach
    void reset() {
        customerClient = new MockServerClient(customerServiceMock.getHost(), customerServiceMock.getServerPort());
        contactClient = new MockServerClient(contactServiceMock.getHost(), contactServiceMock.getServerPort());
        customerClient.reset();
        contactClient.reset();
        keycloakClient = WebClient.builder().baseUrl(keycloak.getAuthServerUrl()).build();
    }

    @Test
    void testRoutingToCustomerService() {
        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Customer response"));

        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForUser("staff", "staff"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Customer response");
    }

    @Test
    void testRoutingToContactService() {
        contactClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Contact response"));

        webTestClient.get()
                .uri("/contacts/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForUser("manager", "manager"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Contact response");
    }

    @Test
    void testFallbackWhenCustomerServiceDown() {
        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(503));

        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForUser("staff", "staff"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Customer service unavailable");
    }

    @Test
    void testServiceDownTriggersCircuitBreaker() {
        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(503));

        for (int i = 0; i < 15; i++) { // Exceed slidingWindowSize to open circuit
            webTestClient.get()
                    .uri("/customers/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getTokenForUser("staff", "staff"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class).isEqualTo("Customer service unavailable");
        }
    }

    @Test
    void testAccessControlWithRoles() {
        String staffToken = getTokenForUser("staff", "staff");
        String managerToken = getTokenForUser("manager", "manager");
        String adminToken = getTokenForUser("admin", "admin");

        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Customer response"));
        contactClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Contact response"));

        // Staff can access /customers
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .exchange()
                .expectStatus().isOk();

        // Staff cannot access /contacts
        webTestClient.get()
                .uri("/contacts/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                .exchange()
                .expectStatus().isForbidden();

        // Manager can access /contacts
        webTestClient.get()
                .uri("/contacts/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk();

        // Manager cannot access /customers
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                .exchange()
                .expectStatus().isForbidden();

        // Admin can access both
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/contacts/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testJwtValidation() {
        String validToken = getTokenForUser("staff", "staff");
        String invalidToken = validToken + "tampered";

        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Customer response"));

        // Valid token
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk();

        // Invalid token
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testRateLimiting() {
        customerClient.when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("Customer response"));

        String token = getTokenForUser("staff", "staff");

        // Send 11 requests quickly (burstCapacity=10)
        for (int i = 0; i < 10; i++) {
            webTestClient.get()
                    .uri("/customers/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk();
        }

        // 11th request should be rate limited
        webTestClient.get()
                .uri("/customers/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
