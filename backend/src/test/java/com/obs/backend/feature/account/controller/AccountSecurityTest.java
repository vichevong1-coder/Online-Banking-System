package com.obs.backend.feature.account.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
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
 * Security tests for account endpoints covering IDOR vulnerabilities,
 * unauthenticated access, and invalid input handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AccountSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void accessingAnotherCustomersTransactionHistoryReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-001", "correct-horse");
        String ownerAccount = openAccount(ownerToken, "SAVINGS", "USD");

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-002", "correct-horse");

        // IDOR: intruder must not see owner's transaction history.
        mockMvc.perform(
                        get("/accounts/" + ownerAccount + "/transactions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void accessingAnotherCustomersBalanceReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-003", "correct-horse");
        String ownerAccount = openAccount(ownerToken, "SAVINGS", "USD");

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-004", "correct-horse");

        mockMvc.perform(
                        get("/accounts/" + ownerAccount + "/balance")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void openAccountEndpointIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/accounts/requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void balanceEndpointIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/accounts/" + UUID.randomUUID() + "/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transactionHistoryEndpointIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/accounts/" + UUID.randomUUID() + "/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAccountTypeEnumReturns400() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-005", "correct-horse");

        mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"INVALID\", \"currency\": \"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCurrencyEnumReturns400() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-006", "correct-horse");

        mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"CRYPTO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyBodyOnAccountOpenReturns400() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-002-007", "correct-horse");

        mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
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
}
