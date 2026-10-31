package com.obs.backend.feature.kyc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.obs.backend.feature.audit.AuditService;
import com.obs.backend.feature.kyc.config.KycProperties;
import com.obs.backend.feature.kyc.dto.KycApplicationResponse;
import com.obs.backend.feature.kyc.entity.KycDocument;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import com.obs.backend.feature.kyc.exception.IncompleteKycDocumentsException;
import com.obs.backend.feature.kyc.exception.KycException;
import com.obs.backend.feature.kyc.mapper.KycMapper;
import com.obs.backend.feature.kyc.repository.KycDocumentRepository;
import com.obs.backend.feature.kyc.service.KycStorageService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.entity.User.Gender;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import com.obs.backend.security.jwt.JwtService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private KycDocumentRepository documentRepository;
    @Mock private KycStorageService storage;
    @Mock private KycMapper mapper;
    @Mock private KycProperties properties;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;

    private KycServiceImpl kycService;

    private final UUID customerId = UUID.randomUUID();
    private final UUID reviewerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        kycService = new KycServiceImpl(
                userRepository,
                documentRepository,
                storage,
                mapper,
                properties,
                jwtService,
                auditService);
    }

    private User createCustomer(AccountStatus status) {
        return new User(
                "Jane",
                "Doe",
                "hash",
                "123456789",
                LocalDate.now().plusYears(5),
                LocalDate.now().minusYears(25),
                Gender.FEMALE,
                "+85512345678",
                Role.CUSTOMER,
                status,
                true);
    }

    @Test
    void approveFailsWhenCustomerNotFound() {
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.approve(customerId, reviewerId))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ke.getCode()).isEqualTo("KYC_APPLICATION_NOT_FOUND");
                });

        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void approveFailsWhenApplicationIsAlreadyActive() {
        User activeUser = createCustomer(AccountStatus.ACTIVE);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> kycService.approve(customerId, reviewerId))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ke.getCode()).isEqualTo("KYC_NOT_PENDING");
                });

        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void approveFailsWhenApplicationIsAlreadyRejected() {
        User rejectedUser = createCustomer(AccountStatus.REJECTED);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(rejectedUser));

        assertThatThrownBy(() -> kycService.approve(customerId, reviewerId))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ke.getCode()).isEqualTo("KYC_NOT_PENDING");
                });

        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void approveFailsWithIncompleteKycDocumentsExceptionWhenDocumentsAreMissing() {
        User pendingUser = createCustomer(AccountStatus.PENDING_APPROVAL);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(pendingUser));

        // Only 2 documents uploaded (< 3 required)
        List<KycDocument> twoDocs = List.of(
                new KycDocument(customerId, KycDocumentType.NID_FRONT, "key1", "image/jpeg", 100, "hash1"),
                new KycDocument(customerId, KycDocumentType.NID_BACK, "key2", "image/jpeg", 100, "hash2"));
        when(documentRepository.findByUserIdOrderByTypeAsc(customerId)).thenReturn(twoDocs);

        assertThatThrownBy(() -> kycService.approve(customerId, reviewerId))
                .isInstanceOf(IncompleteKycDocumentsException.class)
                .satisfies(ex -> {
                    IncompleteKycDocumentsException ike = (IncompleteKycDocumentsException) ex;
                    assertThat(ike.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ike.getCode()).isEqualTo("KYC_DOCUMENTS_INCOMPLETE");
                });

        assertThat(pendingUser.getStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void approveSucceedsWhenPendingAndAllDocumentsPresent() {
        User pendingUser = createCustomer(AccountStatus.PENDING_APPROVAL);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(pendingUser));

        List<KycDocument> allDocs = List.of(
                new KycDocument(customerId, KycDocumentType.NID_FRONT, "key1", "image/jpeg", 100, "hash1"),
                new KycDocument(customerId, KycDocumentType.NID_BACK, "key2", "image/jpeg", 100, "hash2"),
                new KycDocument(customerId, KycDocumentType.SELFIE, "key3", "image/jpeg", 100, "hash3"));
        when(documentRepository.findByUserIdOrderByTypeAsc(customerId)).thenReturn(allDocs);

        KycApplicationResponse expectedResponse = new KycApplicationResponse(
                customerId, "Jane", "Doe", "+85512345678", "123456789",
                LocalDate.now().plusYears(5), LocalDate.now().minusYears(25), Gender.FEMALE,
                AccountStatus.ACTIVE, null, Instant.now(), List.of());
        when(mapper.toApplication(pendingUser, allDocs)).thenReturn(expectedResponse);

        KycApplicationResponse response = kycService.approve(customerId, reviewerId);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(pendingUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(pendingUser.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(pendingUser.getReviewedAt()).isNotNull();
        assertThat(pendingUser.getRejectionReason()).isNull();

        verify(auditService).logAction("KYC_APPROVED", customerId.toString(), "USER", "KYC application approved");
    }

    @Test
    void rejectFailsWhenApplicationIsAlreadyActive() {
        User activeUser = createCustomer(AccountStatus.ACTIVE);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> kycService.reject(customerId, reviewerId, "Invalid documents"))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ke.getCode()).isEqualTo("KYC_NOT_PENDING");
                });

        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void rejectFailsWhenApplicationIsAlreadyRejected() {
        User rejectedUser = createCustomer(AccountStatus.REJECTED);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(rejectedUser));

        assertThatThrownBy(() -> kycService.reject(customerId, reviewerId, "Reason"))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ke.getCode()).isEqualTo("KYC_NOT_PENDING");
                });

        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void rejectFailsWhenReasonIsNull() {
        User pendingUser = createCustomer(AccountStatus.PENDING_APPROVAL);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(pendingUser));

        assertThatThrownBy(() -> kycService.reject(customerId, reviewerId, null))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ke.getCode()).isEqualTo("INVALID_REJECTION_REASON");
                });

        assertThat(pendingUser.getStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void rejectFailsWhenReasonIsBlank() {
        User pendingUser = createCustomer(AccountStatus.PENDING_APPROVAL);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(pendingUser));

        assertThatThrownBy(() -> kycService.reject(customerId, reviewerId, "   "))
                .isInstanceOf(KycException.class)
                .satisfies(ex -> {
                    KycException ke = (KycException) ex;
                    assertThat(ke.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ke.getCode()).isEqualTo("INVALID_REJECTION_REASON");
                });

        assertThat(pendingUser.getStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void rejectSucceedsWhenPendingAndReasonValid() {
        User pendingUser = createCustomer(AccountStatus.PENDING_APPROVAL);
        when(userRepository.findByIdAndRole(customerId, Role.CUSTOMER)).thenReturn(Optional.of(pendingUser));

        List<KycDocument> docs = List.of(
                new KycDocument(customerId, KycDocumentType.NID_FRONT, "key1", "image/jpeg", 100, "hash1"));
        when(documentRepository.findByUserIdOrderByTypeAsc(customerId)).thenReturn(docs);

        KycApplicationResponse expectedResponse = new KycApplicationResponse(
                customerId, "Jane", "Doe", "+85512345678", "123456789",
                LocalDate.now().plusYears(5), LocalDate.now().minusYears(25), Gender.FEMALE,
                AccountStatus.REJECTED, "Blurry photo", Instant.now(), List.of());
        when(mapper.toApplication(pendingUser, docs)).thenReturn(expectedResponse);

        KycApplicationResponse response = kycService.reject(customerId, reviewerId, "  Blurry photo  ");

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(pendingUser.getStatus()).isEqualTo(AccountStatus.REJECTED);
        assertThat(pendingUser.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(pendingUser.getReviewedAt()).isNotNull();
        assertThat(pendingUser.getRejectionReason()).isEqualTo("Blurry photo");

        verify(auditService).logAction("KYC_REJECTED", customerId.toString(), "USER", "KYC application rejected");
    }
}
