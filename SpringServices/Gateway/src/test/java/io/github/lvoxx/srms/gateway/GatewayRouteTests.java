package io.github.lvoxx.srms.gateway;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.lvoxx.srms.gateway.constants.Api2Test;
import io.github.lvoxx.srms.gateway.constants.UserAccount;
import io.github.lvoxx.srms.gateway.testcontainers.AbstractKeycloakAndTestContainers;
import io.github.lvoxx.srms.gateway.utils.MockHttp;
import lombok.extern.java.Log;

@Log
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// @Import(TestGatewayConfig.class)
public class GatewayRouteTests extends AbstractKeycloakAndTestContainers {
        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ReactiveRedisConnectionFactory redisConnectionFactory;

        // Mock client call to service below Gateway
        private static MockServerClient customerClient;
        private static MockServerClient contactClient;

        // For accessing to gateway auth
        private static String staffToken;
        private static String managerToken;
        private static String adminToken;
        private static String validToken;
        private static String invalidToken;

        @BeforeAll
        static void setUp() {
                customerClient = new MockServerClient(customerServiceMock.getHost(),
                                customerServiceMock.getServerPort());
                contactClient = new MockServerClient(contactServiceMock.getHost(), contactServiceMock.getServerPort());
                keycloakClient = WebClient.builder().baseUrl(keycloak.getAuthServerUrl()).build();
        }

        @BeforeEach
        void reset() {
                customerClient.reset();
                log.info("Reset Customer Client");
                contactClient.reset();
                log.info("Reset Contactor Client");

                // Reset Access Token for testing to avoid TOO_MANY_REQUEST
                staffToken = getTokenForUser(UserAccount.STAFF.getUsername(), UserAccount.STAFF.getPassword());
                managerToken = getTokenForUser(UserAccount.MANAGER.getUsername(), UserAccount.MANAGER.getPassword());
                adminToken = getTokenForUser(UserAccount.ADMIN.getUsername(), UserAccount.ADMIN.getPassword());
                validToken = staffToken;
                invalidToken = validToken + "dummy";

                // NOTE: Clear rate limter after a test for SURE
                // redisConnectionFactory.getConnectionFactory().getConnection().flushAll().block();
                // ^^^^^^^^^^^^^^^^ Below Spring-data-redis 3.x ^^^^^^^^^^^^^^^^^^^
                redisConnectionFactory.getReactiveConnection()
                                .serverCommands()
                                .flushAll()
                                .doOnSuccess(aVoid -> log.info("Redis FLUSH OK"))
                                .doOnError(e -> log.log(Level.FINEST, "Redis FLUSH FAILED", e))
                                .block();
        }

        @Test
        void testRoutingToCustomerService() {
                customerClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Customer response"));

                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(String.class).isEqualTo("Customer response");
        }

        @Test
        void testRoutingToContactService() {
                contactClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Contact response"));

                webTestClient.get()
                                .uri(Api2Test.CONTACTORS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(String.class).isEqualTo("Contact response");
        }

        @Test
        void testFallbackWhenCustomerServiceDown() throws InterruptedException {
                // Giai đoạn 1: mock service trả về 503 → để CB ghi nhận lỗi
                MockHttp.callWithCode(customerClient, HttpMethod.GET, Api2Test.SubPath.TEST, 503);
                // Gửi đủ số lần để CB mở (10 requests, 50% lỗi → mở CB)
                for (int i = 0; i < 10; i++) {
                        webTestClient.get()
                                        .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                                        .exchange()
                                        .expectStatus().isEqualTo(503); // Service down nên trả về lỗi
                        Thread.sleep(300); // delay 300ms để tránh vượt replenishRate của rate limiter
                }

                //customerClient.reset();

                // Giai đoạn 2: gửi 1 request để xem CB đã mở chưa (sẽ trả về fallback nếu mở)
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                                .exchange()
                                .expectStatus().isEqualTo(503);
        }

        @Test
        void testAccessControlWithRoles_3rolesCanGainAccessToCustomerService() {
                customerClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Customer response"));

                // Staff
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                                .exchange()
                                .expectStatus().isOk();

                // Manager
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                                .exchange()
                                .expectStatus().isOk();

                // Admin
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .exchange()
                                .expectStatus().isOk();

        }

        @Test
        void testAccessControlWithRoles_staffrolesCanGainAccessToContactorService() {
                contactClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Contact response"));

                // Staff
                webTestClient.get()
                                .uri(Api2Test.CONTACTORS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        void testAccessControlWithRoles_managerroleCanGainAccessToContactorService() {
                contactClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Contact response"));

                // Manager
                webTestClient.get()
                                .uri(Api2Test.CONTACTORS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        void testAccessControlWithRoles_adminroleCanGainAccessToContactorService() {
                contactClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Contact response"));

                // Admin
                webTestClient.get()
                                .uri(Api2Test.CONTACTORS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        void testJwtValidation() {
                customerClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Customer response"));

                // Valid token
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                                .exchange()
                                .expectStatus().isOk();

                // Invalid token
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                                .exchange()
                                .expectStatus().isUnauthorized();
        }

        @Test
        void testRateLimiting() {
                customerClient.when(request().withMethod("GET").withPath(Api2Test.SubPath.TEST.getPath()))
                                .respond(response().withStatusCode(200).withBody("Customer response"));

                String token = validToken;

                // Send 20 requests quickly (burstCapacity=20)
                for (int i = 0; i < 20; i++) {
                        webTestClient.get()
                                        .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .exchange();
                        // .expectStatus().isOk();
                }

                // 26th request should be rate limited
                webTestClient.get()
                                .uri(Api2Test.CUSTOMERS.with(Api2Test.SubPath.TEST))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .exchange()
                                .expectStatus().isEqualTo(429);
        }
}
