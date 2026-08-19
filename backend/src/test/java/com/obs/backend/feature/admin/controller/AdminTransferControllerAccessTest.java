package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminTransferControllerAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void customerTokenIsRejectedFromTransferFeed() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-13-000-090", "correct-horse");

        mockMvc.perform(get("/admin/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/admin/transfers")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminTokenIsAllowed() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/transfers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
