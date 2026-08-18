package com.obs.backend.feature.beneficiary.mapper;

import com.obs.backend.feature.beneficiary.dto.BeneficiaryResponse;
import com.obs.backend.feature.beneficiary.entity.Beneficiary;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryMapper {

    /** userId is deliberately not on the response: every row returned is already the caller's own. */
    public BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getDisplayName(),
                beneficiary.getBankCode(),
                beneficiary.getAccountNumber(),
                beneficiary.isFavorite(),
                beneficiary.getCreatedAt(),
                beneficiary.getUpdatedAt());
    }
}
