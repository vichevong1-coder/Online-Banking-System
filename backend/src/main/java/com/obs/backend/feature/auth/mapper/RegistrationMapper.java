package com.obs.backend.feature.auth.mapper;

import com.obs.backend.feature.auth.dto.RegisterRequest;
import com.obs.backend.feature.auth.dto.RegisterResponse;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public User toEntity(RegisterRequest request, String normalizedPhone, String passwordHash) {
        return new User(
                request.firstName(),
                request.lastName(),
                passwordHash,
                request.nidNumber(),
                request.nidExpiryDate(),
                request.dateOfBirth(),
                request.gender(),
                normalizedPhone,
                Role.CUSTOMER,
                AccountStatus.ACTIVE,
                false);
    }

    public RegisterResponse toResponse(User user) {
        return new RegisterResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getPhone());
    }
}
