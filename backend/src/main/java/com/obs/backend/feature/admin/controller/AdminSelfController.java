package com.obs.backend.feature.admin.controller;

import com.obs.backend.feature.admin.dto.ChangePasswordRequest;
import com.obs.backend.feature.admin.service.AdminSelfService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/me")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSelfController {

    private final AdminSelfService adminSelfService;
    private final CurrentUserProvider currentUserProvider;

    public AdminSelfController(AdminSelfService adminSelfService, CurrentUserProvider currentUserProvider) {
        this.adminSelfService = adminSelfService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        adminSelfService.changePassword(currentUserProvider.currentUserId(), request);
    }
}
