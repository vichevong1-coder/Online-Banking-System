package com.obs.backend.feature.admin.controller;

import com.obs.backend.feature.admin.dto.KpiResponse;
import com.obs.backend.feature.admin.service.AdminKpiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/kpis")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKpiController {

    private final AdminKpiService adminKpiService;

    public AdminKpiController(AdminKpiService adminKpiService) {
        this.adminKpiService = adminKpiService;
    }

    @GetMapping
    public KpiResponse getKpis() {
        return adminKpiService.getKpis();
    }
}
