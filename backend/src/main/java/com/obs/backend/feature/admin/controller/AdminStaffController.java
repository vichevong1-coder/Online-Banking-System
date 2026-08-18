package com.obs.backend.feature.admin.controller;

import com.obs.backend.feature.admin.dto.CreateStaffRequest;
import com.obs.backend.feature.admin.dto.StaffResponse;
import com.obs.backend.feature.admin.dto.UpdateStaffRoleRequest;
import com.obs.backend.feature.admin.service.AdminStaffService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    public AdminStaffController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    @GetMapping
    public List<StaffResponse> listStaff() {
        return adminStaffService.listStaff();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return adminStaffService.createStaff(request);
    }

    @PatchMapping("/{staffId}/role")
    public StaffResponse updateStaffRole(
            @PathVariable UUID staffId,
            @Valid @RequestBody UpdateStaffRoleRequest request) {
        return adminStaffService.updateStaffRole(staffId, request);
    }
}
