package com.obs.backend.feature.statement.service;

import java.time.LocalDate;
import java.util.UUID;

public interface StatementService {

    byte[] generateStatement(UUID userId, UUID accountId, LocalDate fromDate, LocalDate toDate);

    void emailStatement(UUID userId, UUID accountId, LocalDate fromDate, LocalDate toDate);
}
