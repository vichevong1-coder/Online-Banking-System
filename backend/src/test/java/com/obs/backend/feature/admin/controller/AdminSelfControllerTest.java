package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.ADMIN_EMAIL;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminSelfControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void adminCanChangeOwnPassword() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(post("/admin/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"ChangeMe123!\", \"newPassword\": \"NewPassword123!\"}"))
                .andExpect(status().isNoContent());

        // Verify login works with new password
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"NewPassword123!\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void wrongCurrentPasswordFails() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(post("/admin/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"WrongPassword!\", \"newPassword\": \"NewPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void shortNewPasswordFailsValidation() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(post("/admin/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"ChangeMe123!\", \"newPassword\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").isNotEmpty());
    }

    @Test
    void customerTokenIsForbidden() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-15-000-001", "correct-horse");

        mockMvc.perform(post("/admin/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"ChangeMe123!\", \"newPassword\": \"NewPassword123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsForbidden() throws Exception {
        mockMvc.perform(post("/admin/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"ChangeMe123!\", \"newPassword\": \"NewPassword123!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
