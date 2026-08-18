package com.obs.backend.feature.admin.service;

import com.obs.backend.feature.admin.dto.CreateStaffRequest;
import com.obs.backend.feature.admin.dto.StaffResponse;
import com.obs.backend.feature.admin.dto.UpdateStaffRoleRequest;
import java.util.List;
import java.util.UUID;

public interface AdminStaffService {

    List<StaffResponse> listStaff();

    StaffResponse createStaff(CreateStaffRequest request);

    StaffResponse updateStaffRole(UUID staffId, UpdateStaffRoleRequest request);
}
