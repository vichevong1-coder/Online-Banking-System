package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.admin.dto.KpiResponse;
import com.obs.backend.feature.admin.service.AdminKpiService;
import com.obs.backend.feature.auth.repository.LoginAttemptRepository;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.Role;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminKpiServiceImpl implements AdminKpiService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TransferRepository transferRepository;

    public AdminKpiServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            LoginAttemptRepository loginAttemptRepository,
            TransferRepository transferRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KpiResponse getKpis() {
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalAccounts = accountRepository.count();
        long failedLogins = loginAttemptRepository.countBySuccessFalse();

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = startOfDay.plus(Duration.ofDays(1));

        long todayTransfers = transferRepository.countByStatusAndCreatedAtBetween(
                TransferStatus.COMPLETED, startOfDay, startOfNextDay);
        BigDecimal todayVolume = transferRepository.sumAmountByStatusAndCurrencyAndCreatedAtBetween(
                TransferStatus.COMPLETED, Currency.USD, startOfDay, startOfNextDay);
        if (todayVolume == null) {
            todayVolume = BigDecimal.ZERO;
        }
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
