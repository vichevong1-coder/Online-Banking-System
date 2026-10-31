package com.obs.backend.feature.kyc.controller;

import com.obs.backend.feature.kyc.dto.KycUploadResponse;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import com.obs.backend.feature.kyc.service.KycService;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Called by the mobile app during signup, before the customer has an access token. The bearer
 * credential is the short-lived KYC upload token returned by {@code /auth/otp/verify}; the route is
 * permitAll in SecurityConfig and the service validates the token itself.
 */
@RestController
@RequestMapping("/auth/kyc")
public class KycUploadController {

    private final KycService kycService;

    public KycUploadController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping(path = "/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycUploadResponse upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable KycDocumentType type,
            @RequestParam("file") MultipartFile file)
            throws IOException {
        return kycService.upload(authorization, type, file.getBytes());
    }
}
