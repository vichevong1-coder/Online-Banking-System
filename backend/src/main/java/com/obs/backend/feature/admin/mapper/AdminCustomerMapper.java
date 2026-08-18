package com.obs.backend.feature.admin.mapper;

import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdminCustomerMapper {

    public CustomerSummaryResponse toSummary(User user) {
        return new CustomerSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.isPhoneVerified(),
                user.getCreatedAt());
    }

    public CustomerDetailResponse toDetail(User user) {
        return new CustomerDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.isPhoneVerified(),
                user.getNidNumber(),
                user.getNidExpiryDate(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getCreatedAt());
    }
}
