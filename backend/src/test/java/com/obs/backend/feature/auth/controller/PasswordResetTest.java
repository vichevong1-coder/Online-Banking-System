package com.obs.backend.feature.auth.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class PasswordResetTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void forgotAndResetPasswordFlowSucceeds() throws Exception {
        String phone = "+855-12-345-020";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "initial-password!");

        // 1. Request password reset
        mockMvc.perform(
                        post("/auth/password/forgot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"%s\"}".formatted(phone)))
                .andExpect(status().isNoContent());

        String resetCode = otpSender.lastCodeFor(phone);

        // 2. Reset password with code
        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "identifier": "%s",
                                          "code": "%s",
                                          "newPassword": "brand-new-password-123!"
                                        }
                                        """
                                                .formatted(phone, resetCode)))
                .andExpect(status().isNoContent());

        // 3. Login with new password succeeds
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"password\": \"brand-new-password-123!\"}".formatted(phone)))
                .andExpect(status().isOk());
    }

    /**
     * The reset endpoints are reachable without a token, so neither may reveal whether an
     * identifier has an account here. An unknown phone gets the same 204 a real one does,
     * and nothing is sent to it.
     */
    @Test
    void forgotPasswordDoesNotRevealWhetherAnAccountExists() throws Exception {
        String unregistered = "+855-12-345-021";

        mockMvc.perform(
                        post("/auth/password/forgot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\": \"%s\"}".formatted(unregistered)))
                .andExpect(status().isNoContent());

        assertThat(otpSender.lastCodeFor(unregistered)).isNull();
    }

    /**
     * The same oracle, one endpoint along: an unknown identifier has to fail exactly the way
     * a known identifier with a wrong code fails, or closing it on /forgot just moves it here.
     */
    @Test
    void resetPasswordFailsIdenticallyForUnknownIdentifierAndWrongCode() throws Exception {
        String registered = "+855-12-345-022";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, registered, "initial-password!");

        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"identifier": "%s", "code": "000000", "newPassword": "new-password!"}
                                        """.formatted(registered)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_OR_EXPIRED_OTP"));

        mockMvc.perform(
                        post("/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"identifier": "+855-12-345-023", "code": "000000", "newPassword": "new-password!"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_OR_EXPIRED_OTP"));
    }
}
