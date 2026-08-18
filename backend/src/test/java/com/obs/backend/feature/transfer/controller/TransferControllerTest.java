package com.obs.backend.feature.transfer.controller;

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

/**
 * US-025 / US-027 / US-028.
 *
 * <p>Accounts open at a zero balance and Sprint 2 built no deposit endpoint, so
 * funding goes in with a native UPDATE. That has to be bracketed by
 * flush/clear: the account was created through the API inside this same
 * transaction, so it is a managed entity, and a native UPDATE the persistence
 * context never sees would otherwise be shadowed by the cached balance of 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class TransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void transferMovesMoneyBetweenOwnAccountsAndReturnsAReceipt() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-001", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "500.0000");

        MvcResult result = mockMvc.perform(transferRequest(token, source, destination, "120.50", "Rent"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(120.50))
                .andExpect(jsonPath("$.description").value("Rent"))
                .andExpect(jsonPath("$.fromAccountId").value(source))
                .andExpect(jsonPath("$.toAccountId").value(destination))
                .andExpect(jsonPath("$.fromAccountNumber").exists())
                .andExpect(jsonPath("$.toAccountNumber").exists())
                .andExpect(jsonPath("$.reference").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        String transferId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/accounts/" + source + "/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.balance").value(379.50));
        mockMvc.perform(
                        get("/accounts/" + destination + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.balance").value(120.50));

        // Both ledger legs exist, are linked to the transfer, and carry the
        // balance each account was left at.
        mockMvc.perform(
                        get("/accounts/" + source + "/transactions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(379.50));
        mockMvc.perform(
                        get("/accounts/" + destination + "/transactions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER_IN"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(120.50));

        assertLegsLinkedToTransfer(transferId);

        // US-028 receipt.
        mockMvc.perform(get("/transfers/" + transferId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId))
                .andExpect(jsonPath("$.amount").value(120.50))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void aCompletedTransferProducesANotification() throws Exception {
        // US-035 seam: the transfer service goes through the shared
        // NotificationService rather than a path of its own.
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-002", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "500.0000");

        mockMvc.perform(transferRequest(token, source, destination, "25.00", "Coffee fund"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[0].title").value("Transfer completed"));
    }

    @Test
    void aTransferLargerThanTheBalanceIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-003", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "10.0000");

        mockMvc.perform(transferRequest(token, source, destination, "10.01", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));

        mockMvc.perform(get("/accounts/" + source + "/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.balance").value(10.00));
    }

    @Test
    void aTransferBetweenDifferentCurrenciesIsRejectedRatherThanConverted() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-004", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "KHR");
        fund(source, "500.0000");

        mockMvc.perform(transferRequest(token, source, destination, "50.00", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CURRENCY_MISMATCH"));
    }

    @Test
    void aTransferAboveThePerTransferCapIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-005", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "50000.0000");

        // The account is funded well past the amount — this is the cap talking,
        // not the balance. Per-transfer cap is 5,000 USD.
        mockMvc.perform(transferRequest(token, source, destination, "5000.01", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRANSFER_LIMIT_EXCEEDED"));
    }

    @Test
    void transfersThatTogetherExceedTheDailyCapAreRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-006", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "50000.0000");

        // Each is under the 5,000 per-transfer cap; the third crosses the
        // 10,000 daily one.
        mockMvc.perform(transferRequest(token, source, destination, "4000.00", null))
                .andExpect(status().isCreated());
        mockMvc.perform(transferRequest(token, source, destination, "4000.00", null))
                .andExpect(status().isCreated());
        mockMvc.perform(transferRequest(token, source, destination, "4000.00", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DAILY_LIMIT_EXCEEDED"));

        // The rejected one moved nothing.
        mockMvc.perform(get("/accounts/" + source + "/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.balance").value(42000.00));
    }

    @Test
    void aTransferToTheSameAccountIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-007", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        fund(source, "500.0000");

        mockMvc.perform(transferRequest(token, source, source, "10.00", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SAME_ACCOUNT_TRANSFER"));
    }

    @Test
    void transferringFromAnAccountYouDoNotOwnReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-008", "correct-horse");
        String othersAccount = openAccount(ownerToken, "SAVINGS", "USD");
        fund(othersAccount, "500.0000");

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-009", "correct-horse");
        String ownAccount = openAccount(intruderToken, "CHECKING", "USD");

        // 404, not 403 — otherwise a stranger's account id could be confirmed by
        // the status code alone.
        mockMvc.perform(transferRequest(intruderToken, othersAccount, ownAccount, "10.00", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void readingAnotherCustomersTransferReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-010", "correct-horse");
        String source = openAccount(ownerToken, "SAVINGS", "USD");
        String destination = openAccount(ownerToken, "CHECKING", "USD");
        fund(source, "500.0000");
        String transferId = transfer(ownerToken, source, destination, "40.00");

        String otherToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-011", "correct-horse");

        mockMvc.perform(get("/transfers/" + transferId).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRANSFER_NOT_FOUND"));

        // An id that exists for nobody looks exactly the same.
        mockMvc.perform(
                        get("/transfers/" + UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRANSFER_NOT_FOUND"));
    }

    @Test
    void transferEndpointsAreRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": 1}"
                                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/transfers")).andExpect(status().isForbidden());
        mockMvc.perform(get("/transfers/" + UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    void aTransferWithoutARequiredFieldFailsValidation() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-012", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"toAccountId\": \"%s\", \"amount\": 0}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void historyReturnsOnlyTheCallersTransfersNewestFirst() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-013", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "500.0000");

        // Distinguishable amounts rather than trusting timestamp granularity to
        // identify which row is which.
        transfer(token, source, destination, "11.00");
        transfer(token, source, destination, "22.00");
        String newest = transfer(token, source, destination, "33.00");

        String strangerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-25-000-014", "correct-horse");
        String strangerSource = openAccount(strangerToken, "SAVINGS", "USD");
        String strangerDestination = openAccount(strangerToken, "CHECKING", "USD");
        fund(strangerSource, "500.0000");
        transfer(strangerToken, strangerSource, strangerDestination, "99.00");

        mockMvc.perform(get("/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(newest))
                .andExpect(jsonPath("$.content[0].amount").value(33.00))
                .andExpect(jsonPath("$.content[1].amount").value(22.00))
                .andExpect(jsonPath("$.content[2].amount").value(11.00));

        // The stranger sees only their own.
        mockMvc.perform(get("/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(99.00));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder transferRequest(
            String token, String fromAccountId, String toAccountId, String amount, String description) {
        String body = description == null
                ? "{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": %s}"
                        .formatted(fromAccountId, toAccountId, amount)
                : "{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": %s, \"description\": \"%s\"}"
                        .formatted(fromAccountId, toAccountId, amount, description);
        return post("/transfers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String transfer(String token, String fromAccountId, String toAccountId, String amount) throws Exception {
        MvcResult result = mockMvc.perform(transferRequest(token, fromAccountId, toAccountId, amount, null))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String openAccount(String token, String accountType, String currency) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"%s\", \"currency\": \"%s\"}"
                                        .formatted(accountType, currency)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /**
     * There is no deposit endpoint, so opening balances go straight into the row.
     * flush before / clear after: the account is managed in this test's
     * transaction and would otherwise keep serving its cached balance of zero.
     */
    private void fund(String accountId, String balance) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = :balance WHERE id = :accountId")
                .setParameter("balance", new BigDecimal(balance))
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();
    }

    private void assertLegsLinkedToTransfer(String transferId) {
        entityManager.flush();
        Number legs = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM transactions WHERE transfer_id = :transferId")
                .setParameter("transferId", UUID.fromString(transferId))
                .getSingleResult();
        org.assertj.core.api.Assertions.assertThat(legs.intValue()).isEqualTo(2);
    }
}
