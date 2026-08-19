package com.obs.backend.feature.beneficiary.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.beneficiary.dto.BeneficiaryResponse;
import com.obs.backend.feature.beneficiary.dto.CreateBeneficiaryRequest;
import com.obs.backend.feature.beneficiary.dto.UpdateBeneficiaryRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface BeneficiaryService {

    /** US-029: saves a payee for the caller. */
    BeneficiaryResponse addBeneficiary(UUID userId, CreateBeneficiaryRequest request);

    /** US-029: the caller's own saved payees, newest first. */
    PageResponse<BeneficiaryResponse> listBeneficiaries(UUID userId, Pageable pageable);

    /** US-030: partial update — fields the request omits are left as they are. */
    BeneficiaryResponse updateBeneficiary(UUID userId, UUID beneficiaryId, UpdateBeneficiaryRequest request);

    /** US-030: removes one of the caller's own saved payees. */
    void deleteBeneficiary(UUID userId, UUID beneficiaryId);
}
