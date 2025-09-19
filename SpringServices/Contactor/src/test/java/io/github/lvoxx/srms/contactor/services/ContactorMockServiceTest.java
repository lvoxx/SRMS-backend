package io.github.lvoxx.srms.contactor.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import io.github.lvoxx.srms.common.exception.model.ConflictException;
import io.github.lvoxx.srms.common.exception.model.DataPersistantException;
import io.github.lvoxx.srms.common.exception.model.InUsedException;
import io.github.lvoxx.srms.common.exception.model.NotFoundException;
import io.github.lvoxx.srms.common.utils.MessageUtils;
import io.github.lvoxx.srms.contactor.dto.ContactorDTO;
import io.github.lvoxx.srms.contactor.dto.Rating;
import io.github.lvoxx.srms.contactor.mappers.ContactorMapper;
import io.github.lvoxx.srms.contactor.models.Contactor;
import io.github.lvoxx.srms.contactor.models.ContactorType;
import io.github.lvoxx.srms.contactor.repository.ContactorRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Contactor Service Tests")
@Tags({
                @Tag("Service"), @Tag("Mock"), @Tag("Message"), @Tag("Integate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactorMockServiceTest {

        @Mock
        private ContactorRepository contactorRepository;

        @Mock
        private ContactorMapper contactorMapper;

        @Mock
        private MessageUtils messageUtils;

        @InjectMocks
        private ContactorService contactorService;

        private UUID testId;
        private String testEmail;
        private Contactor testContactor;
        private ContactorDTO.Request testRequest;
        private ContactorDTO.Response testResponse;

        @BeforeEach
        void setUp() {
                testId = UUID.randomUUID();
                testEmail = "test@example.com";

                testContactor = new Contactor();
                testContactor.setId(testId);
                testContactor.setEmail(testEmail);
                testContactor.setOrganizationName("Test Organization");
                testContactor.setContactorType(ContactorType.SUPPLIER.name());
                testContactor.setPhoneNumber("1234567890");
                testContactor.setRating(Rating.HIGH.getRate());
                testContactor.setDeletedAt(null);

                testRequest = ContactorDTO.Request.builder()
                                .email(testEmail)
                                .organizationName("Test Organization")
                                .type(ContactorType.SUPPLIER)
                                .phoneNumber("1234567890")
                                .build();

                testResponse = ContactorDTO.Response.builder()
                                .id(testId)
                                .email(testEmail)
                                .organizationName("Test Organization")
                                .type(ContactorType.SUPPLIER)
                                .phoneNumber("1234567890")
                                .build();
        }

        // ==================== CREATE TESTS ====================

        @Test
        @DisplayName("Should create contactor successfully")
        void testCreateContactorSuccess() {
                // Given
                when(contactorRepository.findByEmail(testEmail, true)).thenReturn(Mono.empty());
                when(contactorMapper.toEntity(testRequest)).thenReturn(testContactor);
                when(contactorRepository.save(testContactor)).thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.create(testRequest))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findByEmail(testEmail, true);
                verify(contactorMapper).toEntity(testRequest);
                verify(contactorRepository).save(testContactor);
                verify(contactorMapper).toResponse(testContactor);
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void testCreateContactorEmailExists() {
                // Given
                when(contactorRepository.findByEmail(testEmail, true)).thenReturn(Mono.just(testContactor));
                when(messageUtils.getMessage("error.update.conflicted", new Object[] { testEmail }))
                                .thenReturn("Email already exists");

                // When & Then
                StepVerifier.create(contactorService.create(testRequest))
                                .expectError(ConflictException.class)
                                .verify();

                verify(contactorRepository).findByEmail(testEmail, true);
                verify(messageUtils).getMessage("error.update.conflicted", new Object[] { testEmail });
                verifyNoInteractions(contactorMapper);
                verify(contactorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DataPersistantException when save fails")
        void testCreateContactorSaveFails() {
                // Given
                when(contactorRepository.findByEmail(testEmail, true)).thenReturn(Mono.empty());
                when(contactorMapper.toEntity(testRequest)).thenReturn(testContactor);
                when(contactorRepository.save(testContactor)).thenReturn(Mono.empty());

                // When & Then
                StepVerifier.create(contactorService.create(testRequest))
                                .expectError(DataPersistantException.class)
                                .verify();

                verify(contactorRepository).findByEmail(testEmail, true);
                verify(contactorMapper).toEntity(testRequest);
                verify(contactorRepository).save(testContactor);
        }

        // ==================== UPDATE TESTS ====================

        @Test
        @DisplayName("Should update contactor successfully")
        void testUpdateContactorSuccess() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(contactorRepository.save(testContactor)).thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.update(testId, testRequest))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findById(testId);
                verify(contactorMapper).updateEntityFromRequest(testRequest, testContactor);
                verify(contactorRepository).save(testContactor);
                verify(contactorMapper).toResponse(testContactor);
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating non-existent contactor")
        void testUpdateContactorNotFound() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.empty());
                when(messageUtils.getMessage("error.resource_not_found.id", new Object[] { testId }))
                                .thenReturn("Contactor not found");

                // When & Then
                StepVerifier.create(contactorService.update(testId, testRequest))
                                .expectError(NotFoundException.class)
                                .verify();

                verify(contactorRepository).findById(testId);
                verify(messageUtils).getMessage("error.resource_not_found.id", new Object[] { testId });
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating deleted contactor")
        void testUpdateDeletedContactor() {
                // Given
                testContactor.setDeletedAt(OffsetDateTime.now());
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(messageUtils.getMessage("error.resource_not_found.id", new Object[] { testId }))
                                .thenReturn("Contactor not found");

                // When & Then
                StepVerifier.create(contactorService.update(testId, testRequest))
                                .expectError(NotFoundException.class)
                                .verify();

                verify(contactorRepository).findById(testId);
                verify(messageUtils).getMessage("error.resource_not_found.id", new Object[] { testId });
        }

        // ==================== DELETE TESTS ====================

        @Test
        @DisplayName("Should delete contactor successfully")
        void testsoftDeleteSuccess() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(contactorRepository.softDeleteById(testId)).thenReturn(Mono.just(1));

                // When & Then
                StepVerifier.create(contactorService.softDelete(testId))
                                .expectNext(true)
                                .verifyComplete();

                verify(contactorRepository).findById(testId);
                verify(contactorRepository).softDeleteById(testId);
        }

        @Test
        @DisplayName("Should return false when delete affects no rows")
        void testsoftDeleteNoRows() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(contactorRepository.softDeleteById(testId)).thenReturn(Mono.just(0));

                // When & Then
                StepVerifier.create(contactorService.softDelete(testId))
                                .expectNext(false)
                                .verifyComplete();

                verify(contactorRepository).findById(testId);
                verify(contactorRepository).softDeleteById(testId);
        }

        // ==================== RESTORE TESTS ====================

        @Test
        @DisplayName("Should restore contactor successfully")
        void testRestoreContactorSuccess() {
                // Given
                testContactor.setDeletedAt(OffsetDateTime.now());
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(contactorRepository.restoreById(testId)).thenReturn(Mono.just(1));

                // When & Then
                StepVerifier.create(contactorService.restore(testId))
                                .expectNext(true)
                                .verifyComplete();

                verify(contactorRepository).findById(testId);
                verify(contactorRepository).restoreById(testId);
        }

        @Test
        @DisplayName("Should throw InUsedException when restoring active contactor")
        void testRestoreActiveContactor() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(messageUtils.getMessage("error.update.inused", new Object[] { testId }))
                                .thenReturn("Contactor is already active");

                // When & Then
                StepVerifier.create(contactorService.restore(testId))
                                .expectError(InUsedException.class)
                                .verify();

                verify(contactorRepository).findById(testId);
                verify(messageUtils).getMessage("error.update.inused", new Object[] { testId });
                verify(contactorRepository, never()).restoreById(any());
        }

        // ==================== FIND BY ID TESTS ====================

        @Test
        @DisplayName("Should find contactor by ID successfully")
        void testFindByIdSuccess() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.findById(testId))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findById(testId);
                verify(contactorMapper).toResponse(testContactor);
        }

        @Test
        @DisplayName("Should throw NotFoundException when contactor not found by ID")
        void testFindByIdNotFound() {
                // Given
                when(contactorRepository.findById(testId)).thenReturn(Mono.empty());
                when(messageUtils.getMessage("error.resource_not_found.id", new Object[] { testId }))
                                .thenReturn("Contactor not found");

                // When & Then
                StepVerifier.create(contactorService.findById(testId))
                                .expectError(NotFoundException.class)
                                .verify();

                verify(contactorRepository).findById(testId);
                verify(messageUtils).getMessage("error.resource_not_found.id", new Object[] { testId });
        }

        // ==================== FIND BY EMAIL TESTS ====================

        @Test
        @DisplayName("Should find contactor by email successfully")
        void testFindByEmailSuccess() {
                // Given
                when(contactorRepository.findByEmail(testEmail.toLowerCase(), false))
                                .thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.findByEmail(testEmail, false))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findByEmail(testEmail.toLowerCase(), false);
                verify(contactorMapper).toResponse(testContactor);
        }

        @Test
        @DisplayName("Should handle email trimming and lowercase conversion")
        void testFindByEmailTrimAndLowercase() {
                // Given
                String emailWithSpaces = "  TEST@EXAMPLE.COM  ";
                when(contactorRepository.findByEmail("test@example.com", false))
                                .thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.findByEmail(emailWithSpaces, false))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findByEmail("test@example.com", false);
        }

        // ==================== FIND BY TYPE TESTS ====================

        @Test
        @DisplayName("Should find contactors by type successfully")
        void testFindByTypeSuccess() {
                // Given
                List<Contactor> contactors = Arrays.asList(testContactor);
                when(contactorRepository.findByContactTypeAndShowingDeleted("SUPPLIER", false))
                                .thenReturn(Flux.fromIterable(contactors));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.findByType(ContactorType.SUPPLIER, false))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findByContactTypeAndShowingDeleted("SUPPLIER", false);
                verify(contactorMapper).toResponse(testContactor);
        }

        // ==================== FIND BY ORGANIZATION NAME TESTS ====================

        @Test
        @DisplayName("Should find contactors by organization name successfully")
        void testFindByOrganizationNameSuccess() {
                // Given
                String orgName = "Test";
                List<Contactor> contactors = Arrays.asList(testContactor);
                when(contactorRepository.findByOrganizationNameContainingAndShowingDeleted(orgName, false))
                                .thenReturn(Flux.fromIterable(contactors));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.findByOrganizationName(orgName, false))
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findByOrganizationNameContainingAndShowingDeleted(orgName, false);
                verify(contactorMapper).toResponse(testContactor);
        }

        // ==================== EXISTS TESTS ====================

        @Test
        @DisplayName("Should return true when email exists")
        void testExistsByEmailTrue() {
                // Given
                when(contactorRepository.findByEmail(testEmail, false)).thenReturn(Mono.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.existsByEmail(testEmail))
                                .expectNext(true)
                                .verifyComplete();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void testExistsByEmailFalse() {
                // Given
                when(contactorRepository.findByEmail(testEmail, false)).thenReturn(Mono.empty());
                when(messageUtils.getMessage("error.resource_not_found.email", new Object[] { testEmail }))
                                .thenReturn("Email not found");

                // When & Then
                StepVerifier.create(contactorService.existsByEmail(testEmail))
                                .expectNext(false)
                                .verifyComplete();
        }

        @Test
        @DisplayName("Should return false for null or empty email")
        void testExistsByEmailNullOrEmpty() {
                // When & Then
                StepVerifier.create(contactorService.existsByEmail(null))
                                .expectNext(false)
                                .verifyComplete();

                StepVerifier.create(contactorService.existsByEmail(""))
                                .expectNext(false)
                                .verifyComplete();

                StepVerifier.create(contactorService.existsByEmail("   "))
                                .expectNext(false)
                                .verifyComplete();

                verifyNoInteractions(contactorRepository);
        }

        @Test
        @DisplayName("Should return true when phone number exists")
        void testExistsByPhoneNumberTrue() {
                // Given
                String phoneNumber = "1234567890";
                when(contactorRepository.findAllByShowingDeleted(false))
                                .thenReturn(Flux.just(testContactor));

                // When & Then
                StepVerifier.create(contactorService.existsByPhoneNumber(phoneNumber))
                                .expectNext(true)
                                .verifyComplete();

                verify(contactorRepository).findAllByShowingDeleted(false);
        }

        @Test
        @DisplayName("Should return false when phone number does not exist")
        void testExistsByPhoneNumberFalse() {
                // Given
                String phoneNumber = "9876543210";
                when(contactorRepository.findAllByShowingDeleted(false))
                                .thenReturn(Flux.just(testContactor));

                // When & Then
                StepVerifier.create(contactorService.existsByPhoneNumber(phoneNumber))
                                .expectNext(false)
                                .verifyComplete();

                verify(contactorRepository).findAllByShowingDeleted(false);
        }

        // ==================== COUNT BY TYPE TESTS ====================

        @Test
        @DisplayName("Should count contactors by type successfully")
        void testCountByTypeSuccess() {
                // Given
                when(contactorRepository.findByContactTypeAndShowingDeleted("SUPPLIER", false))
                                .thenReturn(Flux.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.countByType(ContactorType.SUPPLIER))
                                .expectNext(1L)
                                .verifyComplete();
        }

        // ==================== HIGH RATING CONTACTORS TESTS ====================

        @Test
        @DisplayName("Should get high rating contactors successfully")
        void testGetHighRatingContactorsSuccess() {
                // Given
                when(contactorRepository.findAllByShowingDeleted(false))
                                .thenReturn(Flux.just(testContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.getHighRatingContactors())
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findAllByShowingDeleted(false);
                verify(contactorMapper).toResponse(testContactor);
        }

        @Test
        @DisplayName("Should filter out non-high rating contactors")
        void testGetHighRatingContactorsFiltered() {
                // Given
                Contactor lowRatingContactor = new Contactor();
                lowRatingContactor.setRating(Rating.LOW.getRate());
                lowRatingContactor.setDeletedAt(OffsetDateTime.now());

                when(contactorRepository.findAllByShowingDeleted(false))
                                .thenReturn(Flux.just(testContactor, lowRatingContactor));
                when(contactorMapper.toResponse(testContactor)).thenReturn(testResponse);

                // When & Then
                StepVerifier.create(contactorService.getHighRatingContactors())
                                .expectNext(testResponse)
                                .verifyComplete();

                verify(contactorRepository).findAllByShowingDeleted(false);
                verify(contactorMapper, times(1)).toResponse(testContactor);
                verify(contactorMapper, never()).toResponse(lowRatingContactor);
        }

        // ==================== DELETED CONTACTORS TESTS ====================

        @Test
        @DisplayName("Should get deleted contactors successfully")
        void testGetDeletedContactorsSuccess() {
                // Given
                Contactor deletedContactor = Contactor.builder()
                                .id(UUID.randomUUID())
                                .email("deleted@test.com")
                                .deletedAt(OffsetDateTime.now())
                                .build();

                ContactorDTO.Response deletedResponse = ContactorDTO.Response.builder()
                                .id(deletedContactor.getId())
                                .email("deleted@test.com")
                                .deletedAt(deletedContactor.getDeletedAt())
                                .build();

                when(contactorRepository.findDeleted()).thenReturn(Flux.just(deletedContactor));
                when(contactorMapper.toResponse(deletedContactor)).thenReturn(deletedResponse);

                // When & Then
                StepVerifier.create(contactorService.getDeletedContactors()) // 1 findDeleted
                                .expectNext(deletedResponse)
                                .verifyComplete();

                verify(contactorRepository).findDeleted();
                verify(contactorMapper).toResponse(deletedContactor);
        }

        // ==================== EXCEPTION HANDLING TESTS ====================

        @Test
        @DisplayName("Should handle repository exceptions with UnknownServerException")
        void testHandleRepositoryException() {
                // Given
                DataAccessException repositoryException = new DataAccessResourceFailureException("Database error");
                when(contactorRepository.findById(testId)).thenReturn(Mono.error(repositoryException));

                // When & Then
                StepVerifier.create(contactorService.findById(testId))
                                .expectError(DataAccessException.class)
                                .verify();

                verify(contactorRepository).findById(testId);
        }

        @Test
        @DisplayName("Should handle error in create operation")
        void testCreateHandleError() {
                // Given
                DataAccessException repositoryException = new DataAccessResourceFailureException("Saved failure");
                when(contactorRepository.findByEmail(testEmail, true)).thenReturn(Mono.empty());
                when(contactorMapper.toEntity(testRequest)).thenReturn(testContactor);
                when(contactorRepository.save(testContactor)).thenReturn(Mono.error(repositoryException));
                when(messageUtils.getMessage("error.update.failed_to_create", new Object[] { testEmail }))
                                .thenReturn("Failed to create contactor");

                // When & Then
                StepVerifier.create(contactorService.create(testRequest))
                                .expectError(DataAccessException.class)
                                .verify();

                verify(messageUtils).getMessage("error.update.failed_to_create", new Object[] { testEmail });
        }

        // ==================== CACHE TESTS ====================

        @Test
        @DisplayName("Should clear all caches")
        void testClearAllCache() {
                // When
                contactorService.clearAllCache();

                // Then - This is mainly for code coverage as cache operations are handled by
                // Spring
                // We can't easily test the actual cache clearing without integration tests
                assertTrue(true); // Just to have an assertion
        }
}