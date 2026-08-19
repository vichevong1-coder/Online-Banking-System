package com.obs.backend.feature.admin.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
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
class AdminKpiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void adminCanGetKpisWithRealTransfers() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-17-000-001", "correct-horse");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        // US-053 is a system-wide KPI, so it cannot be scoped to this test's own rows the way the
        // transfer feed's tests are. Assert the delta instead: an absolute figure here passes only
        // while the transfers table is empty, and running seed_demo_data.sql breaks it.
        long transfersBefore = kpiLong(adminToken, "$.todayTransfers");
        double volumeBefore = kpiDouble(adminToken, "$.todayVolume");

        String acc1 = openAccount(customerToken, "SAVINGS", "USD");
        String acc2 = openAccount(customerToken, "CHECKING", "USD");
        fund(acc1, "500.00");

        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\": \"%s\", \"toAccountId\": \"%s\", \"amount\": 100.00, \"description\": \"Test\"}".formatted(acc1, acc2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/admin/kpis").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").isNumber())
                .andExpect(jsonPath("$.totalAccounts").isNumber())
                .andExpect(jsonPath("$.failedLogins").isNumber())
                .andExpect(jsonPath("$.todayTransfers").value((int) (transfersBefore + 1)))
                .andExpect(jsonPath("$.todayVolume").value(volumeBefore + 100.0))
                .andExpect(jsonPath("$.displayCurrency").value("USD"));
    }

    @Test
    void customerTokenIsForbidden() throws Exception {
        String customerToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-17-000-002", "correct-horse");

        mockMvc.perform(get("/admin/kpis").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    private long kpiLong(String adminToken, String path) throws Exception {
        return ((Number) readKpi(adminToken, path)).longValue();
    }

    private double kpiDouble(String adminToken, String path) throws Exception {
        return ((Number) readKpi(adminToken, path)).doubleValue();
    }

    private Object readKpi(String adminToken, String path) throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/kpis").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private String openAccount(String token, String accountType, String currency) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"%s\", \"currency\": \"%s\"}"
                                        .formatted(accountType, currency)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void fund(String accountId, String balance) {
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = :balance WHERE id = :accountId")
                .setParameter("balance", new BigDecimal(balance))
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();
    }
}
