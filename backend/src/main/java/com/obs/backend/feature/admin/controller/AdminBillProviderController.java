package com.obs.backend.feature.admin.controller;

import com.obs.backend.feature.bill.dto.BillProviderResponse;
import com.obs.backend.feature.bill.dto.CreateBillProviderRequest;
import com.obs.backend.feature.bill.dto.UpdateBillProviderRequest;
import com.obs.backend.feature.bill.service.BillProviderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bill-providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillProviderController {

    private final BillProviderService billProviderService;

    public AdminBillProviderController(BillProviderService billProviderService) {
        this.billProviderService = billProviderService;
    }

    @GetMapping
    public List<BillProviderResponse> listProviders() {
        return billProviderService.listAllProviders();
    }

    @GetMapping("/{id}")
    public BillProviderResponse getProvider(@PathVariable UUID id) {
        return billProviderService.getProvider(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillProviderResponse createProvider(@Valid @RequestBody CreateBillProviderRequest request) {
        return billProviderService.createProvider(request);
    }

    @PatchMapping("/{id}")
    public BillProviderResponse updateProvider(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBillProviderRequest request) {
        return billProviderService.updateProvider(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProvider(@PathVariable UUID id) {
        billProviderService.deleteProvider(id);
    }
}
