package com.obs.backend.feature.transfer.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
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
 * Security and input validation tests for transfer endpoints.
 * Covers XSS/injection, numeric boundaries, malformed payloads,
 * and missing field scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class TransferInputValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void xssInDescriptionIsStoredLiterallyAndDoesNotCauseA500() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-001", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "500.0000");

        // Script tag payload — must not cause a 500.
        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 10.00,
                                          "description": "<script>alert(1)</script>"
                                        }
                                        """.formatted(source, destination)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("<script>alert(1)</script>"));
    }

    @Test
    void imgOnerrorXssInDescriptionIsStoredLiterallyAndDoesNotCauseA500() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-002", "correct-horse");
        String source = openAccount(token, "SAVINGS", "USD");
        String destination = openAccount(token, "CHECKING", "USD");
        fund(source, "500.0000");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 10.00,
                                          "description": "<img onerror=alert(1) src=x>"
                                        }
                                        """.formatted(source, destination)))
                .andExpect(status().isCreated());
    }

    @Test
    void negativeTransferAmountIsRejectedWithValidationError() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-003", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": -50.00
                                        }
                                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void zeroTransferAmountIsRejectedWithValidationError() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-004", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 0
                                        }
                                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void extremelyLargeAmountIsRejectedNotA500() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-005", "correct-horse");

        // 16 integer digits exceeds @Digits(integer = 15, fraction = 4)
        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 9999999999999999.0001
                                        }
                                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void amountWithMoreThanFourDecimalPlacesIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-006", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 10.12345
                                        }
                                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void descriptionExceeding255CharsIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-007", "correct-horse");
        String longDesc = "A".repeat(256);

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId": "%s",
                                          "toAccountId": "%s",
                                          "amount": 10.00,
                                          "description": "%s"
                                        }
                                        """.formatted(UUID.randomUUID(), UUID.randomUUID(), longDesc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void emptyJsonBodyIsRejectedWithValidationErrors() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-008", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void malformedJsonPayloadReturns400NotA500() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-90-001-009", "correct-horse");

        mockMvc.perform(
                        post("/transfers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fromAccountId\": \"broken json"))
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
