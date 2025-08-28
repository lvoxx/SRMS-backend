package io.github.lvoxx.srms.customer.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.common.exception.controller.GlobalExceptionHandler;
import io.github.lvoxx.srms.common.exception.controller.ValidationExceptionHandler;
import io.github.lvoxx.srms.customer.config.TestControllerWithMessagesConfig;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.dto.CustomerDTO.Response;
import io.github.lvoxx.srms.customer.services.CustomerService;
import reactor.core.publisher.Mono;

@DisplayName("Customer Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@WebFluxTest(controllers = CustomerController.class)
@Import(TestControllerWithMessagesConfig.class)
@ContextConfiguration(classes = {
                GlobalExceptionHandler.class, // Class của ControllerAdvice trong module Common
                ValidationExceptionHandler.class
})
@ActiveProfiles("test")
public class CustomerControllerValidationTest {

        private static final Logger log = LoggerFactory.getLogger(CustomerControllerValidationTest.class);

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ObjectMapper mapper;

        @MockitoBean
        private CustomerService customerService;

        private UUID testId;
        private CustomerDTO.Request validRequest;
        private CustomerDTO.Response validResponse;

        @BeforeEach
        void setUpRequestForTestings() {
                testId = UUID.randomUUID();

                validRequest = CustomerDTO.Request.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("+1234567890")
                                .email("john.doe@example.com")
                                .dietaryRestrictions(new String[] { "Vegetarian" })
                                .allergies(new String[] { "Nuts" })
                                .isRegular(true)
                                .notes("Test notes")
                                .build();

                validResponse = CustomerDTO.Response.builder()
                                .id(testId)
                                .firstName("John")
                                .lastName("Doe")
                                .phoneNumber("+1234567890")
                                .email("john.doe@example.com")
                                .dietaryRestrictions(new String[] { "Vegetarian" })
                                .allergies(new String[] { "Nuts" })
                                .isRegular(true)
                                .notes("Test notes")
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .build();
        }

        @Test
        void testLoadMessages() {
                // Nếu muốn list all, nhưng không direct, có thể load properties thủ công
                ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault()); // Giả sử
                                                                                                   // basename="messages"
                Enumeration<String> keys = bundle.getKeys();
                while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        log.info(key + " = " + bundle.getString(key));
                }
        }

        @Test
        @DisplayName("Should return customer when found")
        void shouldReturnCustomerWhenFound() {
                when(customerService.findById(testId)).thenReturn(Mono.just(validResponse));

                webTestClient.get()
                                .uri("/customers/{id}", testId)
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody(CustomerDTO.Response.class)
                                .consumeWith(res -> {
                                        printPrettyDTOLog(log, res);
                                })
                                .isEqualTo(validResponse);
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void shouldReturn404WhenCustomerNotFound() {
                when(customerService.findById(testId)).thenReturn(Mono.empty());

                webTestClient.get()
                                .uri("/customers/{id}", testId)
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void shouldReturn400ForInvalidUUIDFormat() {
                webTestClient.get()
                                .uri("/customers/invalid-uuid")
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should return paged customers with default parameters")
        void shouldReturnPagedCustomersWithDefaultParameters() {
                PageDTO.PageResponseDTO<CustomerDTO.Response> pageResponse = PageDTO.PageResponseDTO.<CustomerDTO.Response>builder()
                                .content(java.util.List.of(validResponse))
                                .page(0)
                                .size(10)
                                .totalElements(1)
                                .totalPages(1)
                                .build();

                when(customerService.findAllPaged(any(PageDTO.PageRequestDTO.class), eq(false)))
                                .thenReturn(Mono.just(pageResponse));

                webTestClient.get()
                                .uri("/customers?p=0&s=10")
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.content").isArray()
                                .jsonPath("$.content[0].id").isEqualTo(testId.toString())
                                .jsonPath("$.page").isEqualTo(0)
                                .jsonPath("$.size").isEqualTo(10);
        }

        @Test
        @DisplayName("Should handle custom pagination parameters")
        void shouldHandleCustomPaginationParameters() {
                PageDTO.PageResponseDTO<CustomerDTO.Response> pageResponse = PageDTO.PageResponseDTO.<CustomerDTO.Response>builder()
                                .content(java.util.List.of())
                                .page(1)
                                .size(5)
                                .totalElements(0)
                                .totalPages(0)
                                .build();

                when(customerService.findAllPaged(any(PageDTO.PageRequestDTO.class), eq(true)))
                                .thenReturn(Mono.just(pageResponse));

                webTestClient.get()
                                .uri("/customers?p=1&s=5&sb=firstName&o=asc&del=true")
                                .accept(MediaType.APPLICATION_JSON)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should create customer with valid data")
        void shouldCreateCustomerWithValidData() {
                when(customerService.create(any(CustomerDTO.Request.class)))
                                .thenReturn(Mono.just(validResponse));

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(validRequest)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody(CustomerDTO.Response.class)
                                .isEqualTo(validResponse);
        }

        @Test
        @DisplayName("Should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameIsBlank() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .firstName("")
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'firstName')].error")
                                .isEqualTo("First name cannot be empty");
        }

        @Test
        @DisplayName("Should return 400 when lastName is blank")
        void shouldReturn400WhenLastNameIsBlank() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .lastName("   ")
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'lastName')].error")
                                .isEqualTo("Last name cannot be empty");
        }

        @Test
        @DisplayName("Should return 400 when firstName exceeds 50 characters")
        void shouldReturn400WhenFirstNameExceeds50Characters() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .firstName("a".repeat(51))
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'firstName')].error")
                                .isEqualTo("First name must not exceed 50 characters");
        }

        @Test
        @DisplayName("Should return 400 when phoneNumber is blank")
        void shouldReturn400WhenPhoneNumberIsBlank() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .phoneNumber("")
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        printPrettyLog(log, res);
                                })
                                .jsonPath("$.message").isEqualTo("Validation Failure")
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errors").isArray()
                                .jsonPath("$.errors.length()").isEqualTo(2) // Kiểm tra có đúng 2 lỗi
                                .jsonPath("$.errors[?(@.field == 'phoneNumber')].error").value(
                                                Matchers.containsInAnyOrder(
                                                                "Phone number cannot be empty",
                                                                "Invalid phone number format"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "123", "abc", "12345678901234567890", "+abc123", "123-456-7890" })
        @DisplayName("Should return 400 for invalid phone number formats")
        void shouldReturn400ForInvalidPhoneNumberFormats(String invalidPhone) {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .phoneNumber(invalidPhone)
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'phoneNumber')].error")
                                .isEqualTo("Invalid phone number format");
        }

        @ParameterizedTest
        @ValueSource(strings = { "1234567", "+1234567890", "12345678901234", "+123456789012345" })
        @DisplayName("Should accept valid phone number formats")
        void shouldAcceptValidPhoneNumberFormats(String validPhone) {
                CustomerDTO.Request requestWithValidPhone = validRequest.toBuilder()
                                .phoneNumber(validPhone)
                                .build();

                when(customerService.create(any(CustomerDTO.Request.class)))
                                .thenReturn(Mono.just(validResponse));

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestWithValidPhone)
                                .exchange()
                                .expectStatus().isCreated();
        }

        @ParameterizedTest
        @ValueSource(strings = { "invalid-email", "@example.com", "user@", "user@.com" })
        @DisplayName("Should return 400 for invalid email formats")
        void shouldReturn400ForInvalidEmailFormats(String invalidEmail) {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .email(invalidEmail)
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'email')].error").isEqualTo("Invalid email format");
        }

        @Test
        @DisplayName("Should accept empty email")
        void shouldAcceptEmptyEmail() {
                CustomerDTO.Request requestWithEmptyEmail = validRequest.toBuilder()
                                .email("")
                                .build();

                when(customerService.create(any(CustomerDTO.Request.class)))
                                .thenReturn(Mono.just(validResponse));

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(requestWithEmptyEmail)
                                .exchange()
                                .expectStatus().isCreated();
        }

        @Test
        @DisplayName("Should return 400 when email exceeds 100 characters")
        void shouldReturn400WhenEmailExceeds100Characters() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .email("a".repeat(92) + "@test.com") // 101 characters
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        printPrettyLog(log, res);
                                })
                                .jsonPath("$.errors[?(@.field == 'email')].error")
                                .isEqualTo("Email must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should return 400 for multiple validation errors")
        void shouldReturn400ForMultipleValidationErrors() {
                CustomerDTO.Request invalidRequest = CustomerDTO.Request.builder()
                                .firstName("") // blank
                                .lastName("a".repeat(51)) // too long
                                .phoneNumber("abc") // invalid format
                                .email("invalid-email") // invalid format
                                .build();

                webTestClient.post()
                                .uri("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .consumeWith(res -> {
                                        printPrettyLog(log, res);
                                })
                                .jsonPath("$.message").isEqualTo("Validation Failure")
                                .jsonPath("$.status").isEqualTo(400)
                                .jsonPath("$.errors").isArray()
                                .jsonPath("$.errors.length()").isEqualTo(4) // Kiểm tra số lượng lỗi
                                .jsonPath("$.errors[?(@.field == 'lastName')].error")
                                .isEqualTo("Last name must not exceed 50 characters")
                                .jsonPath("$.errors[?(@.field == 'firstName')].error")
                                .isEqualTo("First name cannot be empty")
                                .jsonPath("$.errors[?(@.field == 'email')].error").isEqualTo("Invalid email format")
                                .jsonPath("$.errors[?(@.field == 'phoneNumber')].error")
                                .isEqualTo("Invalid phone number format");
        }

        @Test
        @DisplayName("Should update customer with valid data")
        void shouldUpdateCustomerWithValidData() {
                when(customerService.update(eq(testId), any(CustomerDTO.Request.class)))
                                .thenReturn(Mono.just(validResponse));

                webTestClient.put()
                                .uri("/customers/{id}", testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(validRequest)
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody(CustomerDTO.Response.class)
                                .isEqualTo(validResponse);
        }

        @Test
        @DisplayName("Should return 404 when customer not found for update")
        void shouldReturn404WhenCustomerNotFoundForUpdate() {
                when(customerService.update(eq(testId), any(CustomerDTO.Request.class)))
                                .thenReturn(Mono.empty());

                webTestClient.put()
                                .uri("/customers/{id}", testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(validRequest)
                                .exchange()
                                .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 400 for validation errors during update")
        void shouldReturn400ForValidationErrorsDuringUpdate() {
                CustomerDTO.Request invalidRequest = validRequest.toBuilder()
                                .firstName("")
                                .build();

                webTestClient.put()
                                .uri("/customers/{id}", testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(invalidRequest)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.errors[?(@.field == 'firstName')].error")
                                .isEqualTo("First name cannot be empty");
        }

        @Test
        @DisplayName("Should soft delete customer")
        void shouldSoftDeleteCustomer() {
                when(customerService.softDelete(testId))
                                .thenReturn(Mono.empty());

                webTestClient.delete()
                                .uri("/customers/{id}", testId)
                                .exchange()
                                .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("Should restore customer")
        void shouldRestoreCustomer() {
                when(customerService.restore(testId))
                                .thenReturn(Mono.just(validResponse));

                webTestClient.patch()
                                .uri("/customers/{id}/restore", testId)
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody(CustomerDTO.Response.class)
                                .isEqualTo(validResponse);
        }

        @Test
        @DisplayName("Should return 404 when customer not found for restore")
        void shouldReturn404WhenCustomerNotFoundForRestore() {
                when(customerService.restore(testId))
                                .thenReturn(Mono.empty());

                webTestClient.patch()
                                .uri("/customers/{id}/restore", testId)
                                .exchange()
                                .expectStatus().isNotFound();
        }

        private void printPrettyLog(Logger log, EntityExchangeResult<byte[]> res) {
                try {
                        Object json = mapper.readValue(res.getResponseBody(), Object.class);
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(json));
                } catch (StreamReadException e) {
                        e.printStackTrace();
                } catch (DatabindException e) {
                        e.printStackTrace();
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private void printPrettyDTOLog(Logger log, EntityExchangeResult<Response> res) {
                try {
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(res));
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }
}