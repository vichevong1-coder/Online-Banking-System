package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
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

// US-048. The status column already gates login via AccountStatusPolicy (US-006); until now nothing
// could write it. These tests pin both halves — that the write lands, and that it takes effect.
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminCustomerStatusTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void suspendingACustomerBlocksTheirNextLogin() throws Exception {
        String phone = "+855-15-000-001";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, phone);

        patchStatus(adminToken, customerId, "SUSPENDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // The whole point of the story: the status actually gates the login path.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"correct-horse\"}".formatted(phone)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void reactivatingASuspendedCustomerLetsThemLogInAgain() throws Exception {
        String phone = "+855-15-000-002";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, phone);

        patchStatus(adminToken, customerId, "SUSPENDED").andExpect(status().isOk());
        patchStatus(adminToken, customerId, "ACTIVE").andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"correct-horse\"}".formatted(phone)))
                .andExpect(status().isOk());
    }

    @Test
    void lockingACustomerBlocksLoginWithItsOwnErrorCode() throws Exception {
        String phone = "+855-15-000-003";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, phone);

        patchStatus(adminToken, customerId, "LOCKED").andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"correct-horse\"}".formatted(phone)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));
    }

    @Test
    void anUnknownStatusIsRejected() throws Exception {
        String phone = "+855-15-000-004";
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, phone, "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, phone);

        patchStatus(adminToken, customerId, "DELETED").andExpect(status().isBadRequest());
    }

    // A staff row must not be reachable through the customer endpoints — an admin must not be able to
    // suspend themselves or another admin here.
    @Test
    void aStaffAccountCannotBeSuspendedThroughTheCustomerEndpoint() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String payload = new String(
                java.util.Base64.getUrlDecoder().decode(adminToken.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        String adminId = JsonPath.read(payload, "$.sub");

        patchStatus(adminToken, adminId, "SUSPENDED")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void customerTokenCannotChangeAnyonesStatus() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-15-000-005", "correct-horse");

        mockMvc.perform(patch("/admin/customers/" + UUID.randomUUID() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions patchStatus(
            String adminToken, String customerId, String newStatus) throws Exception {
        return mockMvc.perform(patch("/admin/customers/" + customerId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\"}".formatted(newStatus)));
    }

    private String findCustomerId(String adminToken, String phone) throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/customers?search=" + phone.substring(5))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.content[0].id");
    }
}
