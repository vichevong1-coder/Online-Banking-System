package com.obs.backend.feature.kyc.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.audit.AuditService;
import com.obs.backend.feature.kyc.config.KycProperties;
import com.obs.backend.feature.kyc.dto.KycApplicationResponse;
import com.obs.backend.feature.kyc.dto.KycImage;
import com.obs.backend.feature.kyc.dto.KycUploadResponse;
import com.obs.backend.feature.kyc.dto.PendingKycSummaryResponse;
import com.obs.backend.feature.kyc.entity.KycDocument;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import com.obs.backend.feature.kyc.exception.KycException;
import com.obs.backend.feature.kyc.mapper.KycMapper;
import com.obs.backend.feature.kyc.repository.KycDocumentRepository;
import com.obs.backend.feature.kyc.service.KycService;
import com.obs.backend.feature.kyc.service.KycStorageService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import com.obs.backend.security.jwt.JwtService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class KycServiceImpl implements KycService {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);

    private static final Set<KycDocumentType> REQUIRED = EnumSet.allOf(KycDocumentType.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final UserRepository userRepository;
    private final KycDocumentRepository documentRepository;
    private final KycStorageService storage;
    private final KycMapper mapper;
    private final KycProperties properties;
    private final JwtService jwtService;
    private final AuditService auditService;

    public KycServiceImpl(
            UserRepository userRepository,
            KycDocumentRepository documentRepository,
            KycStorageService storage,
            KycMapper mapper,
            KycProperties properties,
            JwtService jwtService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.mapper = mapper;
        this.properties = properties;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public KycUploadResponse upload(String authorizationHeader, KycDocumentType type, byte[] bytes) {
        UUID userId = resolveUploader(authorizationHeader);
        User user = userRepository.findById(userId).orElseThrow(KycException::invalidUploadToken);
        if (user.getRole() != Role.CUSTOMER || user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw KycException.uploadNotAllowed();
        }

        if (bytes.length == 0 || bytes.length > properties.maxBytes()) {
            throw KycException.invalidDocument("Image must be between 1 byte and " + properties.maxBytes() + " bytes");
        }
        String contentType = detectContentType(bytes);
        String sha256 = sha256Hex(bytes);
        String newKey = storage.store(bytes);

        KycDocument existing = documentRepository.findByUserIdAndType(userId, type).orElse(null);
        String oldKey = null;
        if (existing == null) {
            documentRepository.save(new KycDocument(userId, type, newKey, contentType, bytes.length, sha256));
        } else {
            oldKey = existing.getStorageKey();
            existing.replaceImage(newKey, contentType, bytes.length, sha256);
            documentRepository.save(existing);
        }
        deleteFilesAfterCompletion(oldKey, newKey);

        Set<KycDocumentType> missing = EnumSet.copyOf(REQUIRED);
        documentRepository.findByUserIdOrderByTypeAsc(userId).forEach(d -> missing.remove(d.getType()));
        return new KycUploadResponse(type, List.copyOf(missing));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PendingKycSummaryResponse> listPending(Pageable pageable) {
        return PageResponse.of(userRepository
                .findAwaitingReview(Role.CUSTOMER, AccountStatus.PENDING_APPROVAL, (long) REQUIRED.size(), pageable)
                .map(u -> new PendingKycSummaryResponse(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getPhone(),
                        u.getCreatedAt(),
                        documentRepository.countByUserId(u.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public KycApplicationResponse getApplication(UUID userId) {
        User user = requireCustomer(userId);
        return mapper.toApplication(user, documentRepository.findByUserIdOrderByTypeAsc(userId));
    }

    // Not readOnly: the audit row below is a write.
    @Override
    @Transactional
    public KycImage getImage(UUID userId, KycDocumentType type) {
        requireCustomer(userId);
        KycDocument document =
                documentRepository.findByUserIdAndType(userId, type).orElseThrow(KycException::documentNotFound);
        byte[] bytes = storage.load(document.getStorageKey());
        if (!sha256Hex(bytes).equals(document.getSha256())) {
            throw new IllegalStateException("KYC image " + document.getId() + " does not match its recorded hash");
        }
        auditService.logAction("KYC_IMAGE_VIEWED", userId.toString(), "USER", "Viewed " + type);
        return new KycImage(bytes, document.getContentType());
    }

    @Override
    @Transactional
    public KycApplicationResponse approve(UUID userId, UUID reviewerId) {
        User user = requirePending(userId);
        List<KycDocument> documents = documentRepository.findByUserIdOrderByTypeAsc(userId);
        if (documents.size() < REQUIRED.size()) {
            throw KycException.incomplete();
        }
        user.approveKyc(reviewerId);
        auditService.logAction("KYC_APPROVED", userId.toString(), "USER", "KYC application approved");
        return mapper.toApplication(user, documents);
    }

    @Override
    @Transactional
    public KycApplicationResponse reject(UUID userId, UUID reviewerId, String reason) {
        User user = requirePending(userId);
        user.rejectKyc(reviewerId, reason.trim());
        auditService.logAction("KYC_REJECTED", userId.toString(), "USER", "KYC application rejected");
        return mapper.toApplication(user, documentRepository.findByUserIdOrderByTypeAsc(userId));
    }

    private User requireCustomer(UUID userId) {
        return userRepository.findByIdAndRole(userId, Role.CUSTOMER).orElseThrow(KycException::applicationNotFound);
    }

    private User requirePending(UUID userId) {
        User user = requireCustomer(userId);
        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw KycException.notPending();
        }
        return user;
    }

    private UUID resolveUploader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw KycException.invalidUploadToken();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        try {
            return jwtService
                    .resolveKycUploadSubject(token)
                    .map(UUID::fromString)
                    .orElseThrow(KycException::invalidUploadToken);
        } catch (IllegalArgumentException e) {
            throw KycException.invalidUploadToken();
        }
    }

    /**
     * The file write is not part of the database transaction, so tie it to the outcome: after a
     * commit the replaced file is garbage, after a rollback the new file is.
     */
    private void deleteFilesAfterCompletion(String keyOnCommit, String keyOnRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String key = status == STATUS_COMMITTED ? keyOnCommit : keyOnRollback;
                if (key == null) {
                    return;
                }
                try {
                    storage.delete(key);
                } catch (RuntimeException e) {
                    log.warn("Could not delete orphaned KYC file {}", key, e);
                }
            }
        });
    }

    private static String detectContentType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= PNG_MAGIC.length && Arrays.equals(Arrays.copyOf(bytes, PNG_MAGIC.length), PNG_MAGIC)) {
            return "image/png";
        }
        throw KycException.invalidDocument("Image must be a JPEG or PNG");
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
