package io.github.lvoxx.srms.customer.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.common.exception.controller.GlobalExceptionHandler;
import io.github.lvoxx.srms.common.exception.controller.ValidationExceptionHandler;
import io.github.lvoxx.srms.customer.config.TestControllerOnlyEndpointConfig;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.services.CustomerService;
import reactor.core.publisher.Mono;

@DisplayName("Customer Controller Endpoint Tests")
@Tags({
        @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@WebFluxTest(controllers = CustomerController.class)
@Import(TestControllerOnlyEndpointConfig.class)
@ContextConfiguration(classes = {
        GlobalExceptionHandler.class, // Class của ControllerAdvice trong module Common
        ValidationExceptionHandler.class
})
@ActiveProfiles("test")
@SuppressWarnings("unused")
public class CustomerControllerEndpointTest {
    private static final Logger log = LoggerFactory.getLogger(CustomerControllerEndpointTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CustomerService customerService;

    private CustomerDTO.Response createSampleCustomerResponse() {
        return CustomerDTO.Response.builder()
                .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .email("john.doe@example.com")
                .dietaryRestrictions(new String[] { "vegetarian", "gluten-free" })
                .allergies(new String[] { "nuts", "dairy" })
                .isRegular(true)
                .notes("VIP customer")
                .createdAt(OffsetDateTime.now().minusDays(30))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .deletedAt(null)
                .build();
    }

    private CustomerDTO.Request createSampleCustomerRequest() {
        return CustomerDTO.Request.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+0987654321")
                .email("jane.smith@example.com")
                .dietaryRestrictions(new String[] { "vegan" })
                .allergies(new String[] { "shellfish" })
                .isRegular(false)
                .notes("New customer")
                .build();
    }

    @Nested
    @DisplayName("GET /customers/{id} - Find by ID")
    class FindByIdTests {

        @Test
        @DisplayName("Should return customer with HATEOAS links when found")
        void shouldReturnCustomerWithHateoasLinks() {
            // Given
            UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            CustomerDTO.Response customerResponse = createSampleCustomerResponse();
            when(customerService.findById(customerId)).thenReturn(Mono.just(customerResponse));

            // When & Then
            String responseBody = new String(webTestClient.get()
                    .uri("/customers/{id}", customerId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(customerId.toString())
                    .jsonPath("$.firstName").isEqualTo("John")
                    .jsonPath("$.lastName").isEqualTo("Doe")
                    .jsonPath("$.phoneNumber").isEqualTo("+1234567890")
                    .jsonPath("$.email").isEqualTo("john.doe@example.com")
                    .jsonPath("$.dietaryRestrictions[0]").isEqualTo("vegetarian")
                    .jsonPath("$.dietaryRestrictions[1]").isEqualTo("gluten-free")
                    .jsonPath("$.allergies[0]").isEqualTo("nuts")
                    .jsonPath("$.allergies[1]").isEqualTo("dairy")
                    .jsonPath("$.regular").isEqualTo(true)
                    .jsonPath("$.notes").isEqualTo("VIP customer")
                    .jsonPath("$.createdAt").exists()
                    .jsonPath("$.updatedAt").exists()
                    .jsonPath("$.deletedAt").isEmpty()
                    // HATEOAS Links
                    .jsonPath("$._links.self.href").exists()
                    .jsonPath("$._links['all-customers'].href").exists()
                    .returnResult()
                    .getResponseBodyContent());

            log.info("GET /customers/{} Response:\n", customerId);
            printPrettyLog(log, responseBody);
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void shouldReturn404WhenCustomerNotFound() {
            // Given
            UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
            when(customerService.findById(customerId)).thenReturn(Mono.empty());

            // When & Then
            webTestClient.get()
                    .uri("/customers/{id}", customerId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(Void.class)
                    .consumeWith(response -> {
                        log.info("GET /customers/{} - Not Found Response: Status 404", customerId);
                    });
        }
    }

    @Nested
    @DisplayName("GET /customers - Find all paged")
    class FindAllPagedTests {

        @Test
        @DisplayName("Should return paginated customers")
        void shouldReturnPaginatedCustomers() {
            // Given
            PageDTO.PageRequestDTO pageRequest = new PageDTO.PageRequestDTO(0, 10, "created_by", "desc");
            PageDTO.PageResponseDTO<CustomerDTO.Response> pageResponse = PageDTO.PageResponseDTO.<CustomerDTO.Response>builder()
                    .content(java.util.List.of(createSampleCustomerResponse()))
                    .page(0)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .build();

            when(customerService.findAllPaged(any(PageDTO.PageRequestDTO.class), eq(false)))
                    .thenReturn(Mono.just(pageResponse));

            // When & Then
            String responseBody = new String(webTestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/customers")
                            .queryParam("p", 0)
                            .queryParam("s", 10)
                            .queryParam("sb", "created_by")
                            .queryParam("o", "desc")
                            .queryParam("del", false)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.content").isArray()
                    .jsonPath("$.content[0].id").exists()
                    .jsonPath("$.content[0].firstName").isEqualTo("John")
                    .jsonPath("$.content[0].lastName").isEqualTo("Doe")
                    .jsonPath("$.page").isEqualTo(0)
                    .jsonPath("$.size").isEqualTo(10)
                    .jsonPath("$.totalElements").isEqualTo(1)
                    .jsonPath("$.totalPages").isEqualTo(1)
                    .returnResult()
                    .getResponseBodyContent());

            log.info("GET /customers (paged) Response:\n");
            printPrettyLog(log, responseBody);
        }
    }

    @Nested
    @DisplayName("POST /customers - Create customer")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer and return 201 with HATEOAS links")
        void shouldCreateCustomerWithHateoasLinks() {
            // Given
            CustomerDTO.Request request = createSampleCustomerRequest();
            CustomerDTO.Response response = CustomerDTO.Response.builder()
                    .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .email(request.getEmail())
                    .dietaryRestrictions(request.getDietaryRestrictions())
                    .allergies(request.getAllergies())
                    .isRegular(request.isRegular())
                    .notes(request.getNotes())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .deletedAt(null)
                    .build();

            when(customerService.create(any(CustomerDTO.Request.class))).thenReturn(Mono.just(response));

            // When & Then
            String responseBody = new String(webTestClient.post()
                    .uri("/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectHeader().exists("Location")
                    .expectBody()
                    .jsonPath("$.id").exists()
                    .jsonPath("$.firstName").isEqualTo("Jane")
                    .jsonPath("$.lastName").isEqualTo("Smith")
                    .jsonPath("$.phoneNumber").isEqualTo("+0987654321")
                    .jsonPath("$.email").isEqualTo("jane.smith@example.com")
                    .jsonPath("$.dietaryRestrictions[0]").isEqualTo("vegan")
                    .jsonPath("$.allergies[0]").isEqualTo("shellfish")
                    .jsonPath("$.regular").isEqualTo(false)
                    .jsonPath("$.notes").isEqualTo("New customer")
                    .jsonPath("$.createdAt").exists()
                    .jsonPath("$.updatedAt").exists()
                    .jsonPath("$.deletedAt").isEmpty()
                    // HATEOAS Links
                    .jsonPath("$._links.self.href").exists()
                    .jsonPath("$._links['all-customers'].href").exists()
                    .returnResult()
                    .getResponseBodyContent());

            log.info("POST /customers Response: \n");
            printPrettyLog(log, responseBody);
        }
    }

    @Nested
    @DisplayName("PUT /customers/{id} - Update customer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer and return with HATEOAS links")
        void shouldUpdateCustomerWithHateoasLinks() {
            // Given
            UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            CustomerDTO.Request request = createSampleCustomerRequest();
            CustomerDTO.Response response = CustomerDTO.Response.builder()
                    .id(customerId)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .email(request.getEmail())
                    .dietaryRestrictions(request.getDietaryRestrictions())
                    .allergies(request.getAllergies())
                    .isRegular(request.isRegular())
                    .notes(request.getNotes())
                    .createdAt(OffsetDateTime.now().minusDays(30))
                    .updatedAt(OffsetDateTime.now())
                    .deletedAt(null)
                    .build();

            when(customerService.update(eq(customerId), any(CustomerDTO.Request.class)))
                    .thenReturn(Mono.just(response));

            // When & Then
            String responseBody = new String(webTestClient.put()
                    .uri("/customers/{id}", customerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(customerId.toString())
                    .jsonPath("$.firstName").isEqualTo("Jane")
                    .jsonPath("$.lastName").isEqualTo("Smith")
                    .jsonPath("$.phoneNumber").isEqualTo("+0987654321")
                    .jsonPath("$.email").isEqualTo("jane.smith@example.com")
                    .jsonPath("$.dietaryRestrictions[0]").isEqualTo("vegan")
                    .jsonPath("$.allergies[0]").isEqualTo("shellfish")
                    .jsonPath("$.regular").isEqualTo(false)
                    .jsonPath("$.notes").isEqualTo("New customer")
                    .jsonPath("$.createdAt").exists()
                    .jsonPath("$.updatedAt").exists()
                    .jsonPath("$.deletedAt").isEmpty()
                    // HATEOAS Links
                    .jsonPath("$._links.self.href").exists()
                    .jsonPath("$._links['all-customers'].href").exists()
                    .returnResult()
                    .getResponseBodyContent());

            log.info("PUT /customers/{} Response:\n", customerId);
            printPrettyLog(log, responseBody);
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturn404WhenUpdatingNonExistentCustomer() {
            // Given
            UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
            CustomerDTO.Request request = createSampleCustomerRequest();
            when(customerService.update(eq(customerId), any(CustomerDTO.Request.class)))
                    .thenReturn(Mono.empty());

            // When & Then
            webTestClient.put()
                    .uri("/customers/{id}", customerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(Void.class)
                    .consumeWith(response -> {
                        log.info("PUT /customers/{} - Not Found Response: Status 404", customerId);
                    });
        }
    }

    @Nested
    @DisplayName("DELETE /customers/{id} - Soft delete customer")
    class SoftDeleteCustomerTests {

        @Test
        @DisplayName("Should soft delete customer and return 204")
        void shouldSoftDeleteCustomer() {
            // Given
            UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            when(customerService.softDelete(customerId)).thenReturn(Mono.empty());

            // When & Then
            webTestClient.delete()
                    .uri("/customers/{id}", customerId)
                    .exchange()
                    .expectStatus().isNoContent()
                    .expectBody(Void.class)
                    .consumeWith(response -> {
                        log.info("DELETE /customers/{} Response: Status 204 (No Content)", customerId);
                    });
        }
    }

    private void printPrettyLog(Logger log, String res) {
        try {
            JsonNode jsonNode = mapper.readTree(res); // đọc thẳng thành JsonNode
            String pretty = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonNode);
            log.debug("Response:\n{}", pretty);
        } catch (Exception e) {
            log.error("Failed to pretty print JSON: {}", res, e);
        }
    }

}