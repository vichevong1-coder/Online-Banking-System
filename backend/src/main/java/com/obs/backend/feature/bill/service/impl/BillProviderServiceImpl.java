package com.obs.backend.feature.bill.service.impl;

import com.obs.backend.feature.bill.dto.BillProviderResponse;
import com.obs.backend.feature.bill.dto.CreateBillProviderRequest;
import com.obs.backend.feature.bill.dto.UpdateBillProviderRequest;
import com.obs.backend.feature.bill.entity.BillCategory;
import com.obs.backend.feature.bill.entity.BillProvider;
import com.obs.backend.feature.bill.exception.BillProviderNotFoundException;
import com.obs.backend.feature.bill.repository.BillProviderRepository;
import com.obs.backend.feature.bill.service.BillProviderService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillProviderServiceImpl implements BillProviderService {

    private final BillProviderRepository billProviderRepository;

    public BillProviderServiceImpl(BillProviderRepository billProviderRepository) {
        this.billProviderRepository = billProviderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillProviderResponse> listActiveProviders(BillCategory category) {
        List<BillProvider> providers = category != null
                ? billProviderRepository.findByActiveTrueAndCategoryOrderByNameAsc(category)
                : billProviderRepository.findByActiveTrueOrderByNameAsc();
        return providers.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillProviderResponse> listAllProviders() {
        return billProviderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillProviderResponse getProvider(UUID id) {
        BillProvider provider = billProviderRepository.findById(id).orElseThrow(BillProviderNotFoundException::new);
        return toResponse(provider);
    }

    @Override
    @Transactional
    public BillProviderResponse createProvider(CreateBillProviderRequest request) {
        BillProvider provider = new BillProvider(
                request.name().trim(),
                request.category(),
                request.accountNumberPattern() != null ? request.accountNumberPattern().trim() : null,
                request.active() != null ? request.active() : true
        );
        provider = billProviderRepository.save(provider);
        return toResponse(provider);
    }

    @Override
    @Transactional
    public BillProviderResponse updateProvider(UUID id, UpdateBillProviderRequest request) {
        BillProvider provider = billProviderRepository.findById(id).orElseThrow(BillProviderNotFoundException::new);
        if (request.name() != null && !request.name().isBlank()) {
            provider.setName(request.name().trim());
        }
        if (request.category() != null) {
            provider.setCategory(request.category());
        }
        if (request.accountNumberPattern() != null) {
            provider.setAccountNumberPattern(request.accountNumberPattern().trim());
        }
        if (request.active() != null) {
            provider.setActive(request.active());
        }
        provider = billProviderRepository.save(provider);
        return toResponse(provider);
    }

    @Override
    @Transactional
    public void deleteProvider(UUID id) {
        BillProvider provider = billProviderRepository.findById(id).orElseThrow(BillProviderNotFoundException::new);
        billProviderRepository.delete(provider);
    }

    private BillProviderResponse toResponse(BillProvider p) {
        return new BillProviderResponse(
                p.getId(),
                p.getName(),
                p.getCategory(),
                p.getAccountNumberPattern(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
