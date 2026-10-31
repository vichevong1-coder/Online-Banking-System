package com.obs.backend.feature.kyc.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.kyc.dto.KycApplicationResponse;
import com.obs.backend.feature.kyc.dto.KycImage;
import com.obs.backend.feature.kyc.dto.KycRejectRequest;
import com.obs.backend.feature.kyc.dto.PendingKycSummaryResponse;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import com.obs.backend.feature.kyc.service.KycService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Staff-only, same guard as AdminCustomerController: a CUSTOMER access token is rejected before
// any handler runs.
@RestController
@RequestMapping("/admin/kyc")
@PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
public class AdminKycController {

    private final KycService kycService;
    private final CurrentUserProvider currentUser;

    public AdminKycController(KycService kycService, CurrentUserProvider currentUser) {
        this.kycService = kycService;
        this.currentUser = currentUser;
    }

    @GetMapping("/pending")
    public PageResponse<PendingKycSummaryResponse> pending(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return kycService.listPending(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @GetMapping("/{userId}")
    public KycApplicationResponse application(@PathVariable UUID userId) {
        return kycService.getApplication(userId);
    }

    @GetMapping("/{userId}/documents/{type}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID userId, @PathVariable KycDocumentType type) {
        KycImage image = kycService.getImage(userId, type);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }

    @PostMapping("/{userId}/approve")
    public KycApplicationResponse approve(@PathVariable UUID userId) {
        return kycService.approve(userId, currentUser.currentUserId());
    }

    @PostMapping("/{userId}/reject")
    public KycApplicationResponse reject(@PathVariable UUID userId, @Valid @RequestBody KycRejectRequest request) {
        return kycService.reject(userId, currentUser.currentUserId(), request.reason());
    }
}
