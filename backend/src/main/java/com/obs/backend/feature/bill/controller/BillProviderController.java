package com.obs.backend.feature.bill.controller;

import com.obs.backend.feature.bill.dto.BillProviderResponse;
import com.obs.backend.feature.bill.entity.BillCategory;
import com.obs.backend.feature.bill.service.BillProviderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bill-providers")
public class BillProviderController {

    private final BillProviderService billProviderService;

    public BillProviderController(BillProviderService billProviderService) {
        this.billProviderService = billProviderService;
    }

    @GetMapping
    public List<BillProviderResponse> listProviders(
            @RequestParam(required = false) BillCategory category) {
        return billProviderService.listActiveProviders(category);
    }
}
