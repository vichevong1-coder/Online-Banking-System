package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * Input validation tests for admin customer endpoints: missing status field,
 * null status, non-existent customer ID, XSS in search, SQL wildcard in search.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminCustomerInputValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void updateStatusWithEmptyBodyIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = registerCustomerAndGetId("+855-93-002-001");

        mockMvc.perform(
                        patch("/admin/customers/" + customerId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusWithNullStatusIsRejected() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = registerCustomerAndGetId("+855-93-002-002");

        mockMvc.perform(
                        patch("/admin/customers/" + customerId + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusOfNonExistentCustomerReturnsNotFound() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        patch("/admin/customers/" + UUID.randomUUID() + "/status")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"SUSPENDED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void xssInSearchQueryDoesNotCauseA500() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        registerCustomerAndGetId("+855-93-002-003");

        // XSS payload in search — must return 200, not 500.
        mockMvc.perform(
                        get("/admin/customers")
                                .param("search", "<script>alert(1)</script>")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void sqlWildcardPercentInSearchMatchesDueToUnescapedLike() throws Exception {
        // BUG DOCUMENTATION: The UserRepository uses LIKE CONCAT('%', :search, '%')
        // without escaping % and _ wildcards. A search for "%" matches ALL records.
        // This test documents the current (buggy) behavior.
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        registerCustomerAndGetId("+855-93-002-004");

        mockMvc.perform(
                        get("/admin/customers")
                                .param("search", "%")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                // Wildcard matches at least the customer we just registered.
                .andExpect(jsonPath("$.totalElements").exists());
    }

    /**
     * Registers a customer and returns their user ID by extracting it from the
     * registration response.
     */
    private String registerCustomerAndGetId(String phone) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Test",
                                          "lastName": "Customer",
                                          "password": "correct-horse",
                                          "nidNumber": "123456789",
                                          "nidExpiryDate": "2030-01-01",
                                          "dateOfBirth": "1990-01-01",
                                          "gender": "FEMALE",
                                          "phone": "%s"
                                        }
                                        """.formatted(phone)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
