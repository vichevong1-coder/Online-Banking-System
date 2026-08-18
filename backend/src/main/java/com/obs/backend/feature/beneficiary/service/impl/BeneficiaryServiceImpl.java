package com.obs.backend.feature.beneficiary.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.beneficiary.dto.BeneficiaryResponse;
import com.obs.backend.feature.beneficiary.dto.CreateBeneficiaryRequest;
import com.obs.backend.feature.beneficiary.dto.UpdateBeneficiaryRequest;
import com.obs.backend.feature.beneficiary.entity.Beneficiary;
import com.obs.backend.feature.beneficiary.exception.BeneficiaryNotFoundException;
import com.obs.backend.feature.beneficiary.exception.DuplicateBeneficiaryException;
import com.obs.backend.feature.beneficiary.mapper.BeneficiaryMapper;
import com.obs.backend.feature.beneficiary.repository.BeneficiaryRepository;
import com.obs.backend.feature.beneficiary.service.BeneficiaryService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryMapper beneficiaryMapper;

    public BeneficiaryServiceImpl(BeneficiaryRepository beneficiaryRepository, BeneficiaryMapper beneficiaryMapper) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.beneficiaryMapper = beneficiaryMapper;
    }

    /**
     * US-029. The duplicate check is a coded 409 rather than a bare constraint
     * violation, so the client gets something it can show; the UNIQUE constraint
     * stays as the backstop for the concurrent case.
     */
    @Override
    @Transactional
    public BeneficiaryResponse addBeneficiary(UUID userId, CreateBeneficiaryRequest request) {
        requireDestinationNotAlreadySaved(userId, request.bankCode(), request.accountNumber());

        // saveAndFlush, not save: createdAt/updatedAt are Hibernate timestamps and
        // are only populated once the INSERT is issued, so the response returned
        // below would otherwise carry nulls.
        Beneficiary beneficiary = beneficiaryRepository.saveAndFlush(
                new Beneficiary(userId, request.displayName(), request.bankCode(), request.accountNumber()));
        return beneficiaryMapper.toResponse(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BeneficiaryResponse> listBeneficiaries(UUID userId, Pageable pageable) {
        // Scoped by owner in the query itself, so another customer's rows are not
        // filtered out after the fact — they are never loaded.
        Page<Beneficiary> page = beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page.map(beneficiaryMapper::toResponse));
    }

    /**
     * US-030. Partial by construction: each field is applied only when the
     * request carried it, so a rename cannot blank out the destination and
     * flipping the US-031 favorite flag cannot blank out the name.
     */
    @Override
    @Transactional
    public BeneficiaryResponse updateBeneficiary(
            UUID userId, UUID beneficiaryId, UpdateBeneficiaryRequest request) {
        Beneficiary beneficiary = findOwnedBeneficiary(userId, beneficiaryId);

        if (request.displayName() != null) {
            beneficiary.rename(request.displayName());
        }

        // Either half of the destination may be sent on its own; the missing half
        // keeps its current value, and the pair is then checked as a whole.
        if (request.bankCode() != null || request.accountNumber() != null) {
            String bankCode = request.bankCode() == null ? beneficiary.getBankCode() : request.bankCode();
            String accountNumber =
                    request.accountNumber() == null ? beneficiary.getAccountNumber() : request.accountNumber();

            // Only when it actually moves — re-sending the same destination is a
            // no-op edit, not a collision with itself.
            boolean destinationChanged =
                    !bankCode.equals(beneficiary.getBankCode()) || !accountNumber.equals(beneficiary.getAccountNumber());
            if (destinationChanged) {
                requireDestinationNotAlreadySaved(userId, bankCode, accountNumber);
                beneficiary.changeDestination(bankCode, accountNumber);
            }
        }

        if (request.favorite() != null) {
            // US-031's only write path. Deliberately not its own endpoint: the
            // Sprint 5 quick-transfer list needs the flag, not an API for it.
            beneficiary.setFavorite(request.favorite());
        }

        // Flushed here so @UpdateTimestamp has run before the row is mapped.
        return beneficiaryMapper.toResponse(beneficiaryRepository.saveAndFlush(beneficiary));
    }

    @Override
    @Transactional
    public void deleteBeneficiary(UUID userId, UUID beneficiaryId) {
        // Loaded owner-scoped first rather than deleteById: a blind delete of
        // another customer's id would return 204 and tell the caller it existed.
        beneficiaryRepository.delete(findOwnedBeneficiary(userId, beneficiaryId));
    }

    /** 404 for a beneficiary that isn't the caller's, indistinguishable from one that doesn't exist. */
    private Beneficiary findOwnedBeneficiary(UUID userId, UUID beneficiaryId) {
        return beneficiaryRepository
                .findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(BeneficiaryNotFoundException::new);
    }

    private void requireDestinationNotAlreadySaved(UUID userId, String bankCode, String accountNumber) {
        if (beneficiaryRepository.existsByUserIdAndBankCodeAndAccountNumber(userId, bankCode, accountNumber)) {
            throw new DuplicateBeneficiaryException();
        }
    }
}
