package com.obs.backend.feature.statement.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Security tests for statement endpoints: IDOR on PDF download and email trigger,
 * unauthenticated access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class StatementSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void downloadingAnotherUsersStatementReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-003-001", "correct-horse");
        String ownerAccount = openAccount(ownerToken);

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-003-002", "correct-horse");

        // IDOR: intruder must not download owner's statement.
        mockMvc.perform(
                        get("/accounts/" + ownerAccount + "/statement")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void triggeringEmailStatementForAnotherUsersAccountReturnsNotFound() throws Exception {
        String ownerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-003-003", "correct-horse");
        String ownerAccount = openAccount(ownerToken);

        // Set owner's email so the email path doesn't fail with NO_PROFILE_EMAIL
        mockMvc.perform(
                        patch("/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"owner@test.com\"}"))
                .andExpect(status().isOk());

        String intruderToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-003-004", "correct-horse");

        // IDOR: intruder must not trigger owner's statement email.
        mockMvc.perform(
                        post("/statements/" + ownerAccount + "/email")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void emailStatementEndpointIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(post("/statements/" + UUID.randomUUID() + "/email"))
                .andExpect(status().isUnauthorized());
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
}
