package com.obs.backend.feature.auth.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for password reset flow: weak passwords, wrong codes,
 * missing fields, and password length boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class PasswordResetSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void resetWithPasswordTooShortIsRejected() throws Exception {
        String phone = "+855-91-002-001";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");

        // Trigger forgot password
        mockMvc.perform(
                        post("/auth/password/forgot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"%s\"}".formatted(phone)))
                .andExpect(status().isNoContent());

        String code = otpSender.lastCodeFor(phone);

        // Reset with 7-char password (min is 8)
        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "identifier": "%s",
                                          "code": "%s",
                                          "newPassword": "short12"
                                        }
                                        """.formatted(phone, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void resetWithPasswordTooLongIsRejected() throws Exception {
        String phone = "+855-91-002-002";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");

        mockMvc.perform(
                        post("/auth/password/forgot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"%s\"}".formatted(phone)))
                .andExpect(status().isNoContent());

        String code = otpSender.lastCodeFor(phone);

        // 101-char password (max is 100)
        String longPassword = "A".repeat(101);
        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "identifier": "%s",
                                          "code": "%s",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(phone, code, longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void resetWithWrongCodeIsRejected() throws Exception {
        String phone = "+855-91-002-003";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");

        mockMvc.perform(
                        post("/auth/password/forgot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"%s\"}".formatted(phone)))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "identifier": "%s",
                                          "code": "000000",
                                          "newPassword": "new-secure-password"
                                        }
                                        """.formatted(phone)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetWithMissingFieldsIsRejected() throws Exception {
        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
