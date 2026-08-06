package com.obs.backend.feature.account.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void newCustomerHasNoLinkedAccounts() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-001", "correct-horse");

        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listAccountsIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/accounts")).andExpect(status().isForbidden());
    }

    @Test
    void openingAnAccountCreatesItWithZeroBalanceAndItThenAppearsInTheList() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-002", "correct-horse");

        MvcResult openResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andReturn();
        String accountId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(accountId));
    }

    @Test
    void getBalanceReturnsTheAccountsCurrentBalance() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-003", "correct-horse");
        String accountId = openAccount(token, "CHECKING", "KHR");

        mockMvc.perform(
                        get("/accounts/" + accountId + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("KHR"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void getBalanceOnAnotherCustomersAccountReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-004", "correct-horse");
        String accountId = openAccount(ownerToken, "SAVINGS", "USD");

        String otherToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-005", "correct-horse");

        mockMvc.perform(
                        get("/accounts/" + accountId + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void listTransactionsFiltersByTypeAndAmount() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-006", "correct-horse");
        String accountId = openAccount(token, "SAVINGS", "USD");
        seedTransaction(accountId, "DEPOSIT", "100.00", "100.00");
        seedTransaction(accountId, "WITHDRAWAL", "40.00", "60.00");

        mockMvc.perform(
                        get("/accounts/" + accountId + "/transactions?type=DEPOSIT")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(
                        get("/accounts/" + accountId + "/transactions?minAmount=50")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));
    }

    @Test
    void listTransactionsExcludesATransactionExactlyAtTheToDateBoundary() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-000-007", "correct-horse");
        String accountId = openAccount(token, "SAVINGS", "USD");

        // toDate=2026-01-01 means "up to end of 2026-01-01" — a transaction stamped exactly at
        // the following midnight (the start of 2026-01-02) must not be included.
        Instant exactlyAtNextDayMidnight =
                java.time.LocalDate.of(2026, 1, 2).atStartOfDay(ZoneOffset.UTC).toInstant();
        seedTransactionAt(accountId, "DEPOSIT", "10.00", "10.00", exactlyAtNextDayMidnight);

        mockMvc.perform(
                        get("/accounts/" + accountId + "/transactions?toDate=2026-01-01")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(
                        get("/accounts/" + accountId + "/transactions?toDate=2026-01-02")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private String openAccount(String token, String accountType, String currency) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"accountType\": \"%s\", \"currency\": \"%s\"}"
                                                .formatted(accountType, currency)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void seedTransaction(String accountId, String type, String amount, String balanceAfter) {
        entityManager
                .createNativeQuery(
                        "INSERT INTO transactions (id, account_id, type, amount, currency, balance_after) "
                                + "VALUES (gen_random_uuid(), :accountId, :type, :amount, 'USD', :balanceAfter)")
                .setParameter("accountId", UUID.fromString(accountId))
                .setParameter("type", type)
                .setParameter("amount", new BigDecimal(amount))
                .setParameter("balanceAfter", new BigDecimal(balanceAfter))
                .executeUpdate();
    }

    private void seedTransactionAt(
            String accountId, String type, String amount, String balanceAfter, Instant createdAt) {
        entityManager
                .createNativeQuery(
                        "INSERT INTO transactions (id, account_id, type, amount, currency, balance_after, created_at) "
                                + "VALUES (gen_random_uuid(), :accountId, :type, :amount, 'USD', :balanceAfter, :createdAt)")
                .setParameter("accountId", UUID.fromString(accountId))
                .setParameter("type", type)
                .setParameter("amount", new BigDecimal(amount))
                .setParameter("balanceAfter", new BigDecimal(balanceAfter))
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }
}
