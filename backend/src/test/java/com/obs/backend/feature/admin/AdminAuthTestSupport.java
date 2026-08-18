package com.obs.backend.feature.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Logs in as the bootstrap admin seeded by V2__seed_bootstrap_admin.sql. The credentials are the
// throwaway local-demo defaults in application.properties (there is no test-specific override), so
// this deliberately depends on those defaults — if they change, this constant changes with them.
public final class AdminAuthTestSupport {

    public static final String ADMIN_EMAIL = "admin@obs.local";
    public static final String ADMIN_PHONE = "+855000000000";
    public static final String ADMIN_PASSWORD = "ChangeMe123!";

    private AdminAuthTestSupport() {
    }

    public static String adminLoginAndGetAccessToken(MockMvc mockMvc, RecordingOtpSender otpSender) throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\": \"%s\", \"password\": \"%s\"}"
                                                .formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String challengeToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.challengeToken");
        String code = otpSender.lastCodeFor(ADMIN_PHONE);

        MvcResult verifyResult = mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(challengeToken, code)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(verifyResult.getResponse().getContentAsString(), "$.accessToken");
    }
}
