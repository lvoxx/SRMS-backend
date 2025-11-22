package io.github.lvoxx.srms.contactor.controllers;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import io.github.lvoxx.srms.controllerhandler.controller.GlobalExceptionHandler;
import io.github.lvoxx.srms.controllerhandler.controller.ValidationExceptionHandler;
import io.github.lvoxx.srms.controllerhandler.model.ConflictException;
import io.github.lvoxx.srms.controllerhandler.model.DataPersistantException;
import io.github.lvoxx.srms.controllerhandler.model.NotFoundException;
import io.github.lvoxx.srms.contactor.config.TestControllerWithMessagesConfig;
import io.github.lvoxx.srms.contactor.dto.ContactorDTO;
import io.github.lvoxx.srms.contactor.dto.ContactorDTO.Response;
import io.github.lvoxx.srms.contactor.dto.Rating;
import io.github.lvoxx.srms.contactor.models.ContactorType;
import io.github.lvoxx.srms.contactor.services.ContactorService;
import reactor.core.publisher.Mono;

@DisplayName("Contactor Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@WebFluxTest(controllers = ContactorController.class)
@Import(TestControllerWithMessagesConfig.class)
@ContextConfiguration(classes = {
                GlobalExceptionHandler.class,
                ValidationExceptionHandler.class
})
@ActiveProfiles("test")
@SuppressWarnings("null")
public class ContactorControllerValidationTest {

        private static final Logger log = LoggerFactory.getLogger(ContactorControllerValidationTest.class);

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private ObjectMapper mapper;

        @MockitoBean
        private ContactorService contactorService;

        private UUID testId;
        private ContactorDTO.Request validRequest;
        private ContactorDTO.Response validResponse;

        @BeforeEach
        void setUp() {
                testId = UUID.randomUUID();

                validRequest = ContactorDTO.Request.builder()
                                .type(ContactorType.SUPPLIER)
                                .organizationName("Test Organization")
                                .fullname("John Doe")
                                .phoneNumber("+1234567890")
                                .email("john@example.com")
                                .address("123 Test Street")
                                .rating(Rating.HIGH)
                                .notes("Test notes")
                                .build();

                validResponse = ContactorDTO.Response.builder()
                                .id(testId)
                                .type(ContactorType.SUPPLIER)
                                .organizationName("Test Organization")
                                .fullName("John Doe")
                                .phoneNumber("+1234567890")
                                .email("john@example.com")
                                .address("123 Test Street")
                                .rating(Rating.HIGH)
                                .notes("Test notes")
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .deletedAt(null)
                                .build();
        }

        @Nested
        @DisplayName("DTO Validation Tests")
        class DTOValidationTests {

                @Test
                @DisplayName("Should return 400 when contactType is null")
                void shouldReturn400WhenContactTypeIsNull() throws Exception {
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .type(null)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> printPrettyLog(log, res))
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.type").isEqualTo("Contact type is required");
                }

                @Test
                @DisplayName("Should return 400 when phoneNumber is blank")
                void shouldReturn400WhenPhoneNumberIsBlank() throws Exception {
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .phoneNumber("")
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.phoneNumber").value(
                                                        Matchers.anyOf(
                                                                        Matchers.equalTo("Phone number is required"),
                                                                        Matchers.equalTo(
                                                                                        "Phone number must be 7-15 digits, optionally starting with +")));
                }

                @Test
                @DisplayName("Should return 400 when phoneNumber has invalid pattern")
                void shouldReturn400WhenPhoneNumberHasInvalidPattern() throws Exception {
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .phoneNumber("invalid-phone")
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.phoneNumber").isEqualTo(
                                                        "Phone number must be 7-15 digits, optionally starting with +");
                }

                @Test
                @DisplayName("Should return 400 when email has invalid format")
                void shouldReturn400WhenEmailHasInvalidFormat() throws Exception {
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .email("invalid-email")
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.email").isEqualTo(
                                                        "Invalid email format");
                }

                @Test
                @DisplayName("Should return 400 when organizationName exceeds max length")
                void shouldReturn400WhenOrganizationNameExceedsMaxLength() throws Exception {
                        String longName = "a".repeat(101); // 101 characters
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .organizationName(longName)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.organizationName").isEqualTo(
                                                        "Organization name must be between 1 and 100 characters");
                }

                @Test
                @DisplayName("Should return 400 when fullname exceeds max length")
                void shouldReturn400WhenFullnameExceedsMaxLength() throws Exception {
                        String longName = "a".repeat(51); // 51 characters
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .fullname(longName)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.fullname").isEqualTo(
                                                        "First name must be between 1 and 50 characters");
                }

                @Test
                @DisplayName("Should return 400 when email exceeds max length")
                void shouldReturn400WhenEmailExceedsMaxLength() throws Exception {
                        String longEmail = "a".repeat(90) + "@test.com"; // > 100 characters
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .email(longEmail)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.email").isEqualTo(
                                                        "Invalid email format");
                }

                @Test
                @DisplayName("Should return 400 when address exceeds max length")
                void shouldReturn400WhenAddressExceedsMaxLength() throws Exception {
                        String longAddress = "a".repeat(201); // 201 characters
                        ContactorDTO.Request invalidRequest = validRequest.toBuilder()
                                        .address(longAddress)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.address").isEqualTo(
                                                        "Address must be between 1 and 200 characters");
                }

                @Test
                @DisplayName("Should accept valid request without validation errors")
                void shouldAcceptValidRequestWithoutValidationErrors() throws Exception {
                        Mockito.when(contactorService.create(Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.just(validResponse));

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(validRequest))
                                        .exchange()
                                        .expectStatus().isCreated();
                }
        }

        @Nested
        @DisplayName("Service Exception Tests")
        class ServiceExceptionTests {

                @Test
                @DisplayName("Should return 404 when contactor not found by ID")
                void shouldReturn404WhenContactorNotFoundById() {
                        UUID nonExistentId = UUID.randomUUID();
                        Mockito.when(contactorService.findById(nonExistentId))
                                        .thenReturn(
                                                        Mono.error(new NotFoundException("Contactor not found with id "
                                                                        + nonExistentId + ".")));

                        webTestClient.get()
                                        .uri("/contactors/{id}", nonExistentId)
                                        .exchange()
                                        .expectStatus().isNotFound()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .isEqualTo(
                                                        "Contactor not found with id " + nonExistentId + ".");
                }

                @Test
                @DisplayName("Should return 404 when updating non-existent contactor")
                void shouldReturn404WhenUpdatingNonExistentContactor() throws Exception {
                        UUID nonExistentId = UUID.randomUUID();
                        Mockito.when(contactorService.update(Mockito.eq(nonExistentId),
                                        Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.error(new NotFoundException(
                                                        "Contactor not found with id " + nonExistentId
                                                                        + ". Or already deleted.")));

                        webTestClient.put()
                                        .uri("/contactors/{id}", nonExistentId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(validRequest))
                                        .exchange()
                                        .expectStatus().isNotFound()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(org.hamcrest.Matchers.containsString(
                                                        "Contactor not found with id " + nonExistentId));
                }

                @Test
                @DisplayName("Should return 409 when creating contactor with existing email")
                void shouldReturn409WhenCreatingContactorWithExistingEmail() throws Exception {
                        String existingEmail = "existing@example.com";
                        ContactorDTO.Request conflictRequest = validRequest.toBuilder()
                                        .email(existingEmail)
                                        .build();

                        Mockito.when(contactorService.create(Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.error(
                                                        new ConflictException("Contactor with email " + existingEmail
                                                                        + " is already existed.")));

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(conflictRequest))
                                        .exchange()
                                        .expectStatus().isEqualTo(409)
                                        .expectBody()
                                        .jsonPath("$.details").value(Matchers
                                                        .containsString("Contactor with email " + existingEmail
                                                                        + " is already existed"));
                }

                @Test
                @DisplayName("Should return 500 when create operation fails")
                void shouldReturn500WhenCreateOperationFails() throws Exception {
                        String testEmail = "test@example.com";
                        ContactorDTO.Request createRequest = validRequest.toBuilder()
                                        .email(testEmail)
                                        .build();

                        Mockito.when(contactorService.create(Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.error(
                                                        new DataPersistantException(
                                                                        "Failed to create contactor with email "
                                                                                        + testEmail + ".")));

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(createRequest))
                                        .exchange()
                                        .expectStatus().is5xxServerError()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString(
                                                        "Failed to create contactor with email " + testEmail));
                }

                @Test
                @DisplayName("Should return 500 when update operation fails")
                void shouldReturn500WhenUpdateOperationFails() throws Exception {
                        String testEmail = "test@example.com";
                        ContactorDTO.Request updateRequest = validRequest.toBuilder()
                                        .email(testEmail)
                                        .build();

                        Mockito.when(contactorService.update(Mockito.eq(testId),
                                        Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.error(
                                                        new DataPersistantException(
                                                                        "Failed to update contactor with email "
                                                                                        + testEmail + ".")));

                        webTestClient.put()
                                        .uri("/contactors/{id}", testId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(updateRequest))
                                        .exchange()
                                        .expectStatus().is5xxServerError()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString(
                                                        "Failed to update contactor with email " + testEmail));
                }

                @Test
                @DisplayName("Should return 500 when delete operation fails")
                void shouldReturn500WhenDeleteOperationFails() {
                        Mockito.when(contactorService.softDelete(testId))
                                        .thenReturn(Mono
                                                        .error(new DataPersistantException(
                                                                        "Failed to delete contactor with id " + testId
                                                                                        + ".")));

                        webTestClient.delete()
                                        .uri("/contactors/{id}", testId)
                                        .exchange()
                                        .expectStatus().is5xxServerError()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString(
                                                        "Failed to delete contactor with id " + testId));
                }

                @Test
                @DisplayName("Should return 500 when restore operation fails")
                void shouldReturn500WhenRestoreOperationFails() {
                        Mockito.when(contactorService.restore(testId))
                                        .thenReturn(Mono.error(
                                                        new DataPersistantException(
                                                                        "Failed to restore contactor with email "
                                                                                        + testId + ".")));

                        webTestClient.patch()
                                        .uri("/contactors/{id}/restore", testId)
                                        .exchange()
                                        .expectStatus().is5xxServerError()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString("Failed to restore contactor"));
                }

                @Test
                @DisplayName("Should return 404 when deleting non-existent contactor")
                void shouldReturn404WhenDeletingNonExistentContactor() {
                        UUID nonExistentId = UUID.randomUUID();
                        Mockito.when(contactorService.softDelete(nonExistentId))
                                        .thenReturn(
                                                        Mono.error(new NotFoundException("Contactor not found with id "
                                                                        + nonExistentId + ".")));

                        webTestClient.delete()
                                        .uri("/contactors/{id}", nonExistentId)
                                        .exchange()
                                        .expectStatus().isNotFound()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString(
                                                        "Contactor not found with id " + nonExistentId));
                }

                @Test
                @DisplayName("Should return 404 when restoring non-existent contactor")
                void shouldReturn404WhenRestoringNonExistentContactor() {
                        UUID nonExistentId = UUID.randomUUID();
                        Mockito.when(contactorService.restore(nonExistentId))
                                        .thenReturn(
                                                        Mono.error(new NotFoundException("Contactor not found with id "
                                                                        + nonExistentId + ".")));

                        webTestClient.patch()
                                        .uri("/contactors/{id}/restore", nonExistentId)
                                        .exchange()
                                        .expectStatus().isNotFound()
                                        .expectBody()
                                        .jsonPath("$.details")
                                        .value(Matchers.containsString(
                                                        "Contactor not found with id " + nonExistentId));
                }
        }

        @Nested
        @DisplayName("Edge Cases Tests")
        class EdgeCasesTests {

                @Test
                @DisplayName("Should return 400 when request body is malformed JSON")
                void shouldReturn400WhenRequestBodyIsMalformedJSON() {
                        String malformedJson = "{ invalid json }";

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(malformedJson)
                                        .exchange()
                                        .expectStatus().isBadRequest();
                }

                @Test
                @DisplayName("Should return 400 when request body is empty")
                void shouldReturn400WhenRequestBodyIsEmpty() {
                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .exchange()
                                        .expectStatus().isBadRequest();
                }

                @Test
                @DisplayName("Should return 400 when ID parameter is invalid UUID")
                void shouldReturn400WhenIdParameterIsInvalidUUID() {
                        String invalidUuid = "invalid-uuid";

                        webTestClient.get()
                                        .uri("/contactors/{id}", invalidUuid)
                                        .exchange()
                                        .expectStatus().isBadRequest();
                }

                @Test
                @DisplayName("Should handle multiple validation errors")
                void shouldHandleMultipleValidationErrors() throws Exception {
                        ContactorDTO.Request invalidRequest = ContactorDTO.Request.builder()
                                        .type(null) // Required field missing
                                        .phoneNumber("") // Blank phone number
                                        .email("invalid-email") // Invalid email format
                                        .organizationName("a".repeat(101)) // Too long
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(invalidRequest))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .consumeWith(res -> {
                                                printPrettyLog(log, res);
                                        })
                                        .jsonPath("$.message").exists()
                                        .jsonPath("$.timestamp").exists()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(5)
                                        .jsonPath("$.errors.phoneNumber")
                                        .value(Matchers.anyOf(
                                                        Matchers.equalTo("Phone number is required"),
                                                        Matchers.equalTo(
                                                                        "Phone number must be 7-15 digits, optionally starting with +")))
                                        .jsonPath("$.errors.rating").isEqualTo("Rating is required")
                                        .jsonPath("$.errors.type").isEqualTo("Contact type is required")
                                        .jsonPath("$.errors.email").isEqualTo("Invalid email format")
                                        .jsonPath("$.errors.organizationName")
                                        .isEqualTo("Organization name must be between 1 and 100 characters");
                }
        }

        @Nested
        @DisplayName("Boundary Value Tests")
        class BoundaryValueTests {

                @Test
                @DisplayName("Should accept minimum valid phone number length")
                void shouldAcceptMinimumValidPhoneNumberLength() throws Exception {
                        ContactorDTO.Request requestWithMinPhone = validRequest.toBuilder()
                                        .phoneNumber("1234567") // 7 digits (minimum)
                                        .build();

                        Mockito.when(contactorService.create(Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.just(validResponse));

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(requestWithMinPhone))
                                        .exchange()
                                        .expectStatus().isCreated();
                }

                @Test
                @DisplayName("Should accept maximum valid phone number length")
                void shouldAcceptMaximumValidPhoneNumberLength() throws Exception {
                        ContactorDTO.Request requestWithMaxPhone = validRequest.toBuilder()
                                        .phoneNumber("+123456789012345") // 15 digits (maximum) with +
                                        .build();

                        Mockito.when(contactorService.create(Mockito.any(ContactorDTO.Request.class)))
                                        .thenReturn(Mono.just(validResponse));

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(requestWithMaxPhone))
                                        .exchange()
                                        .expectStatus().isCreated();
                }

                @Test
                @DisplayName("Should reject phone number below minimum length")
                void shouldRejectPhoneNumberBelowMinimumLength() throws Exception {
                        ContactorDTO.Request requestWithShortPhone = validRequest.toBuilder()
                                        .phoneNumber("123456") // 6 digits (below minimum)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(requestWithShortPhone))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.phoneNumber").isEqualTo(
                                                        "Phone number must be 7-15 digits, optionally starting with +");
                }

                @Test
                @DisplayName("Should reject phone number above maximum length")
                void shouldRejectPhoneNumberAboveMaximumLength() throws Exception {
                        ContactorDTO.Request requestWithLongPhone = validRequest.toBuilder()
                                        .phoneNumber("1234567890123456") // 16 digits (above maximum)
                                        .build();

                        webTestClient.post()
                                        .uri("/contactors/")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(mapper.writeValueAsString(requestWithLongPhone))
                                        .exchange()
                                        .expectStatus().isBadRequest()
                                        .expectBody()
                                        .jsonPath("$.errors").isMap()
                                        .jsonPath("$.errors.length()").isEqualTo(1)
                                        .jsonPath("$.errors.phoneNumber").isEqualTo(
                                                        "Phone number must be 7-15 digits, optionally starting with +");
                }
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

        @SuppressWarnings("unused")
        private void printPrettyDTOLog(Logger log, EntityExchangeResult<Response> res) {
                try {
                        log.debug("Response:\n{}", mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(res));
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }
}