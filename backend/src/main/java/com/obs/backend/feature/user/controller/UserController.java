package com.obs.backend.feature.user.controller;

import com.obs.backend.feature.user.dto.ChangePasswordRequest;
import com.obs.backend.feature.user.dto.UpdateProfileRequest;
import com.obs.backend.feature.user.dto.UserProfileResponse;
import com.obs.backend.feature.user.service.UserService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public UserProfileResponse getProfile() {
        return userService.getProfile(currentUserProvider.currentUserId());
    }

    @PatchMapping
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserProvider.currentUserId(), request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserProvider.currentUserId(), request);
    }
}
