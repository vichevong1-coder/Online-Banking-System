package com.obs.backend.feature.beneficiary.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.beneficiary.dto.BeneficiaryResponse;
import com.obs.backend.feature.beneficiary.dto.CreateBeneficiaryRequest;
import com.obs.backend.feature.beneficiary.dto.UpdateBeneficiaryRequest;
import com.obs.backend.feature.beneficiary.service.BeneficiaryService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final CurrentUserProvider currentUserProvider;

    public BeneficiaryController(BeneficiaryService beneficiaryService, CurrentUserProvider currentUserProvider) {
        this.beneficiaryService = beneficiaryService;
        this.currentUserProvider = currentUserProvider;
    }

    /** US-029: saves a payee for the caller. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse addBeneficiary(@Valid @RequestBody CreateBeneficiaryRequest request) {
        return beneficiaryService.addBeneficiary(currentUserProvider.currentUserId(), request);
    }

    /** US-029: the caller's own saved payees, newest first. */
    @GetMapping
    public PageResponse<BeneficiaryResponse> listBeneficiaries(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return beneficiaryService.listBeneficiaries(currentUserProvider.currentUserId(), pageable);
    }

    /**
     * US-030 edit. PATCH rather than PUT because the body is a partial update:
     * omitted fields keep their current values.
     */
    @PatchMapping("/{beneficiaryId}")
    public BeneficiaryResponse updateBeneficiary(
            @PathVariable UUID beneficiaryId, @Valid @RequestBody UpdateBeneficiaryRequest request) {
        return beneficiaryService.updateBeneficiary(currentUserProvider.currentUserId(), beneficiaryId, request);
    }

    /** US-030 delete. 204 with no body; nothing references a beneficiary, so nothing is orphaned. */
    @DeleteMapping("/{beneficiaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(@PathVariable UUID beneficiaryId) {
        beneficiaryService.deleteBeneficiary(currentUserProvider.currentUserId(), beneficiaryId);
    }
}
