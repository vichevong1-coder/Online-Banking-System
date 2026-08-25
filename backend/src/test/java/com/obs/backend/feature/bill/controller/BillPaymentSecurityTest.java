package com.obs.backend.feature.bill.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
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
 * Security tests for bill payment endpoints: IDOR on payment and receipt,
 * negative/zero amounts, missing fields, unauthenticated access,
 * insufficient funds.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class BillPaymentSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void payingBillFromAnotherUsersAccountReturnsNotFound() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String providerId = createProvider(adminToken, "IDOR Provider", "ELECTRICITY");

        // User A opens and funds account
        String userAToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-001", "correct-horse");
        String userAAccount = openAccount(userAToken);
        fund(userAAccount, "500.0000");

        // User B tries to pay from User A's account
        String userBToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-002", "correct-horse");

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": 20.00,
                                          "currency": "USD"
                                        }
                                        """.formatted(userAAccount, providerId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void accessingAnotherUsersPaymentReceiptReturnsNotFound() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String providerId = createProvider(adminToken, "Receipt Provider", "WATER");

        String userAToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-003", "correct-horse");
        String userAAccount = openAccount(userAToken);
        fund(userAAccount, "500.0000");

        // User A pays bill
        MvcResult payResult = mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": 30.00,
                                          "currency": "USD"
                                        }
                                        """.formatted(userAAccount, providerId)))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = JsonPath.read(payResult.getResponse().getContentAsString(), "$.id");

        // User B tries to access the receipt
        String userBToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-004", "correct-horse");

        mockMvc.perform(
                        get("/bill-payments/" + paymentId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void negativeAmountIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String providerId = createProvider(adminToken, "Neg Provider", "ELECTRICITY");

        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-005", "correct-horse");
        String accountId = openAccount(token);

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": -10.00,
                                          "currency": "USD"
                                        }
                                        """.formatted(accountId, providerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void zeroAmountIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String providerId = createProvider(adminToken, "Zero Provider", "ELECTRICITY");

        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-006", "correct-horse");
        String accountId = openAccount(token);

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": 0,
                                          "currency": "USD"
                                        }
                                        """.formatted(accountId, providerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void missingRequiredFieldsReturns400() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-007", "correct-horse");

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.providerId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.currency").exists());
    }

    @Test
    void billPaymentEndpointsAreRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/bill-payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bill-payments"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bill-payments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void payingBillWithInsufficientFundsIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String providerId = createProvider(adminToken, "Funds Provider", "ELECTRICITY");

        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-003-008", "correct-horse");
        String accountId = openAccount(token);
        // Account has zero balance — no funding.

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": 50.00,
                                          "currency": "USD"
                                        }
                                        """.formatted(accountId, providerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    // --- Helpers ---

    private String createProvider(String adminToken, String name, String category) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "%s", "category": "%s", "accountNumberPattern": "^[0-9]{8,12}$"}
                                        """.formatted(name, category)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String openAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void fund(String accountId, String balance) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = :balance WHERE id = :accountId")
                .setParameter("balance", new BigDecimal(balance))
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();
    }
}
