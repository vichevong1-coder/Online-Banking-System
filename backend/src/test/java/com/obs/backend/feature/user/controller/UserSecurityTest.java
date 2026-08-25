package com.obs.backend.feature.user.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for user profile endpoints: invalid email format,
 * mass assignment prevention, password boundaries, unauthenticated access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class UserSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void invalidEmailFormatOnProfileUpdateIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-91-003-001", "correct-horse");

        mockMvc.perform(
                        patch("/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void massAssignmentOfRoleViaProfileUpdateIsIgnored() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-91-003-002", "correct-horse");

        // Attempt to elevate role via profile update
        mockMvc.perform(
                        patch("/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"valid@example.com\", \"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("valid@example.com"));

        // Verify role is still CUSTOMER
        mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void passwordTooShortOnChangeIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-91-003-003", "correct-horse");

        mockMvc.perform(
                        post("/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\": \"correct-horse\", \"newPassword\": \"short12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void passwordTooLongOnChangeIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-91-003-004", "correct-horse");
        String longPassword = "A".repeat(101);

        mockMvc.perform(
                        post("/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\": \"correct-horse\", \"newPassword\": \"%s\"}"
                                        .formatted(longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void missingFieldsOnPasswordChangeIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-91-003-005", "correct-horse");

        mockMvc.perform(
                        post("/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfileIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfileIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        patch("/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"test@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\": \"a\", \"newPassword\": \"b\"}"))
                .andExpect(status().isUnauthorized());
    }
}
