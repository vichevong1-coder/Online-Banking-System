package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminCustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void customerListShowsRegisteredCustomers() throws Exception {
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-14-000-001", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone == '+855-14-000-001')]").isNotEmpty());
    }

    // users holds customers and staff in one table. If the role filter is ever dropped, the
    // bootstrap admin appears in the staff-facing customer list — this is the regression guard.
    @Test
    void customerListExcludesStaffAccounts() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone == '+855000000000')]").isEmpty());
    }

    @Test
    void customerListCanBeSearchedByNameAndPhone() throws Exception {
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-14-000-002", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/customers?search=14-000-002")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].phone").value("+855-14-000-002"));

        mockMvc.perform(get("/admin/customers?search=Jane").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone == '+855-14-000-002')]").isNotEmpty());
    }

    // The list DTO must never carry the password hash or the KYC identifiers.
    @Test
    void customerListNeverExposesSecretsOrKycData() throws Exception {
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-14-000-003", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].nidNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].dateOfBirth").doesNotExist());
    }

    @Test
    void customerDetailReturnsKycDataButNeverThePasswordHash() throws Exception {
        registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-14-000-004", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, "+855-14-000-004");

        mockMvc.perform(get("/admin/customers/" + customerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+855-14-000-004"))
                .andExpect(jsonPath("$.nidNumber").value("123456789"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void customerAccountsListsThatCustomersAccounts() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-14-000-005", "correct-horse");
        mockMvc.perform(post("/accounts/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated());

        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        String customerId = findCustomerId(adminToken, "+855-14-000-005");

        mockMvc.perform(get("/admin/customers/" + customerId + "/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    void unknownCustomerIdIsNotFound() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(get("/admin/customers/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    // A staff id must not resolve through a customer endpoint, or the customer screens become a
    // back door onto staff records.
    @Test
    void staffIdDoesNotResolveThroughTheCustomerEndpoint() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);
        // The admin's own id is the JWT subject — decode it from the token payload.
        String payload = new String(
                Base64.getUrlDecoder().decode(adminToken.split("\\.")[1]), StandardCharsets.UTF_8);
        String adminId = JsonPath.read(payload, "$.sub");

        mockMvc.perform(get("/admin/customers/" + adminId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    private String findCustomerId(String adminToken, String phone) throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/customers?search=" + phone.substring(5))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.content[0].id");
    }
}
