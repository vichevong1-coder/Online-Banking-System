package com.obs.backend.feature.admin.mapper;

import com.obs.backend.feature.admin.dto.StaffResponse;
import com.obs.backend.feature.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdminStaffMapper {

    public StaffResponse toResponse(User user) {
        return new StaffResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
