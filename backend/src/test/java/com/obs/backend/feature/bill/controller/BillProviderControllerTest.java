package com.obs.backend.feature.bill.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class BillProviderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void customerCanListActiveProviders() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-030", "password123!");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"EDC Test\", \"category\": \"ELECTRICITY\", \"accountNumberPattern\": \"^[0-9]{8}$\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/bill-providers").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'EDC Test')]").exists());
    }

    @Test
    void customerGets403OnAdminBillProviders() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-031", "password123!");

        mockMvc.perform(get("/admin/bill-providers").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"EDC Hacker\", \"category\": \"ELECTRICITY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPerformCrudOnBillProviders() throws Exception {
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        // 1. Create
        MvcResult createResult = mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"PPWSA Admin Test\", \"category\": \"WATER\", \"accountNumberPattern\": \"^[0-9]{10}$\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PPWSA Admin Test"))
                .andExpect(jsonPath("$.category").value("WATER"))
                .andReturn();
        String providerId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        // 2. Get
        mockMvc.perform(get("/admin/bill-providers/" + providerId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PPWSA Admin Test"));

        // 3. Update
        mockMvc.perform(
                        patch("/admin/bill-providers/" + providerId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"PPWSA Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PPWSA Updated"));

        // 4. Delete
        mockMvc.perform(delete("/admin/bill-providers/" + providerId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 5. Get after delete returns 404
        mockMvc.perform(get("/admin/bill-providers/" + providerId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
