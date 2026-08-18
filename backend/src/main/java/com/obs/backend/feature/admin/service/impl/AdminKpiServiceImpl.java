package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.admin.dto.KpiResponse;
import com.obs.backend.feature.admin.service.AdminKpiService;
import com.obs.backend.feature.auth.repository.LoginAttemptRepository;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.Role;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminKpiServiceImpl implements AdminKpiService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    public AdminKpiServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            LoginAttemptRepository loginAttemptRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KpiResponse getKpis() {
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalAccounts = accountRepository.count();
        long failedLogins = loginAttemptRepository.countBySuccessFalse();
        // Today's transfer count and volume are stubbed until Sprint 4 when the transfers table lands.
        long todayTransfers = 0;
        BigDecimal todayVolume = BigDecimal.ZERO;
        String displayCurrency = "USD";

        return new KpiResponse(
                totalCustomers,
                totalAccounts,
                failedLogins,
                todayTransfers,
                todayVolume,
                displayCurrency);
    }
}
