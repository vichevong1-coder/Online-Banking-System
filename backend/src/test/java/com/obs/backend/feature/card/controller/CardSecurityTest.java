package com.obs.backend.feature.card.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * Security tests for card endpoints: IDOR on create/block/unblock/update,
 * PIN validation, limit validation, unauthenticated access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class CardSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    // --- IDOR Tests ---

    @Test
    void creatingCardOnAnotherUsersAccountReturnsNotFound() throws Exception {
        String userAToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-001", "correct-horse");
        String userBToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-002", "correct-horse");
        String userAAccount = openAccount(userAToken, "SAVINGS", "USD");

        // User B tries to create a card on User A's account
        mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"1234\"}".formatted(userAAccount)))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockingAnotherUsersCardReturnsNotFound() throws Exception {
        String userAToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-003", "correct-horse");
        String userBToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-004", "correct-horse");
        String cardId = createCard(userAToken);

        mockMvc.perform(
                        post("/cards/" + cardId + "/block")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unblockingAnotherUsersCardReturnsNotFound() throws Exception {
        String userAToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-005", "correct-horse");
        String userBToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-006", "correct-horse");
        String cardId = createCard(userAToken);

        // Block it first
        mockMvc.perform(post("/cards/" + cardId + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk());

        // User B tries to unblock
        mockMvc.perform(
                        post("/cards/" + cardId + "/unblock")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingAnotherUsersCardReturnsNotFound() throws Exception {
        String userAToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-007", "correct-horse");
        String userBToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-008", "correct-horse");
        String cardId = createCard(userAToken);

        mockMvc.perform(
                        patch("/cards/" + cardId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\": \"9999\"}"))
                .andExpect(status().isNotFound());
    }

    // --- PIN Validation ---

    @Test
    void nonNumericPinOnCreateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-009", "correct-horse");
        String accountId = openAccount(token, "SAVINGS", "USD");

        mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"abcd\"}".formatted(accountId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pin").exists());
    }

    @Test
    void pinTooShortOnCreateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-010", "correct-horse");
        String accountId = openAccount(token, "SAVINGS", "USD");

        mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"123\"}".formatted(accountId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pin").exists());
    }

    @Test
    void pinTooLongOnCreateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-011", "correct-horse");
        String accountId = openAccount(token, "SAVINGS", "USD");

        mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"12345\"}".formatted(accountId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pin").exists());
    }

    @Test
    void invalidPinOnUpdateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-012", "correct-horse");
        String cardId = createCard(token);

        mockMvc.perform(
                        patch("/cards/" + cardId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\": \"abcd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pin").exists());
    }

    // --- Limit Validation ---

    @Test
    void negativeDailyLimitIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-013", "correct-horse");
        String cardId = createCard(token);

        mockMvc.perform(
                        patch("/cards/" + cardId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dailyLimit\": -100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dailyLimit").exists());
    }

    @Test
    void zeroPerTransactionLimitIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-014", "correct-horse");
        String cardId = createCard(token);

        mockMvc.perform(
                        patch("/cards/" + cardId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"perTransactionLimit\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.perTransactionLimit").exists());
    }

    // --- Missing fields ---

    @Test
    void missingAccountIdOnCreateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-001-015", "correct-horse");

        mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\": \"1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.accountId").exists());
    }

    // --- Unauthenticated access ---

    @Test
    void cardEndpointsAreRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/cards")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cards/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/cards/" + UUID.randomUUID() + "/block"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private String createCard(String token) throws Exception {
        String accountId = openAccount(token, "SAVINGS", "USD");
        MvcResult result = mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"1234\"}".formatted(accountId)))
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
}
