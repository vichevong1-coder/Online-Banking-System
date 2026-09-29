package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.feature.admin.dto.CreateStaffRequest;
import com.obs.backend.feature.admin.dto.StaffResponse;
import com.obs.backend.feature.admin.dto.UpdateStaffRoleRequest;
import com.obs.backend.feature.admin.exception.EmailAlreadyRegisteredException;
import com.obs.backend.feature.admin.exception.StaffNotFoundException;
import com.obs.backend.feature.admin.mapper.AdminStaffMapper;
import com.obs.backend.feature.admin.service.AdminStaffService;
import com.obs.backend.feature.auth.exception.PhoneAlreadyRegisteredException;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStaffServiceImpl implements AdminStaffService {

    private final UserRepository userRepository;
    private final AdminStaffMapper adminStaffMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminStaffServiceImpl(
            UserRepository userRepository,
            AdminStaffMapper adminStaffMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminStaffMapper = adminStaffMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff() {
        return userRepository.findByRoleInOrderByCreatedAtDesc(List.of(Role.ADMIN, Role.TELLER)).stream()
                .map(adminStaffMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new PhoneAlreadyRegisteredException();
        }

        User staff = User.createStaff(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                passwordEncoder.encode(request.password()),
                request.role());

        User saved = userRepository.saveAndFlush(staff);
        return adminStaffMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StaffResponse updateStaffRole(UUID staffId, UpdateStaffRoleRequest request) {
        User staff = userRepository.findByIdAndRoleIn(staffId, List.of(Role.ADMIN, Role.TELLER))
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        staff.changeRole(request.role());
        User saved = userRepository.save(staff);
        return adminStaffMapper.toResponse(saved);
    }
}
