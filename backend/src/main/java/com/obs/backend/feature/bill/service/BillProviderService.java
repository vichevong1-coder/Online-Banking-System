package com.obs.backend.feature.bill.service;

import com.obs.backend.feature.bill.dto.BillProviderResponse;
import com.obs.backend.feature.bill.dto.CreateBillProviderRequest;
import com.obs.backend.feature.bill.dto.UpdateBillProviderRequest;
import com.obs.backend.feature.bill.entity.BillCategory;
import java.util.List;
import java.util.UUID;

public interface BillProviderService {
    List<BillProviderResponse> listActiveProviders(BillCategory category);
    List<BillProviderResponse> listAllProviders();
    BillProviderResponse getProvider(UUID id);
    BillProviderResponse createProvider(CreateBillProviderRequest request);
    BillProviderResponse updateProvider(UUID id, UpdateBillProviderRequest request);
    void deleteProvider(UUID id);
}
