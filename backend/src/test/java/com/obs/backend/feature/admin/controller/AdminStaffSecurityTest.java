package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for admin staff endpoints: access control, input validation,
 * missing fields, invalid enums, and field length limits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminStaffSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    // --- Access Control ---

    @Test
    void createStaffIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        post("/admin/staff")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createStaffIsRejectedWithCustomerToken() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-001-001", "correct-horse");

        // Must send a valid body so validation passes and the authorization check (403) fires.
        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "User",
                                          "email": "staff-test@test.com",
                                          "phone": "+855-93-100-099",
                                          "role": "ADMIN",
                                          "password": "password123!"
                                        }
                                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStaffRoleIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(
                        patch("/admin/staff/" + UUID.randomUUID() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStaffRoleIsRejectedWithCustomerToken() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-93-001-002", "correct-horse");

        mockMvc.perform(
                        patch("/admin/staff/" + UUID.randomUUID() + "/role")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Input Validation ---

    @Test
    void createStaffWithEmptyBodyReturns400() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void createStaffWithInvalidEmailIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "User",
                                          "email": "not-an-email",
                                          "phone": "+855-93-100-001",
                                          "role": "ADMIN",
                                          "password": "password123!"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createStaffWithPasswordTooShortIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "User",
                                          "email": "staff@test.com",
                                          "phone": "+855-93-100-002",
                                          "role": "ADMIN",
                                          "password": "short12"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void createStaffWithPasswordTooLongIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String longPassword = "A".repeat(101);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "User",
                                          "email": "staff2@test.com",
                                          "phone": "+855-93-100-003",
                                          "role": "ADMIN",
                                          "password": "%s"
                                        }
                                        """.formatted(longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void createStaffWithInvalidRoleEnumIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "User",
                                          "email": "staff3@test.com",
                                          "phone": "+855-93-100-004",
                                          "role": "SUPERUSER",
                                          "password": "password123!"
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStaffWithFirstNameExceeding255CharsIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String longName = "A".repeat(256);

        mockMvc.perform(
                        post("/admin/staff")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "%s",
                                          "lastName": "User",
                                          "email": "staff4@test.com",
                                          "phone": "+855-93-100-005",
                                          "role": "ADMIN",
                                          "password": "password123!"
                                        }
                                        """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }
}
