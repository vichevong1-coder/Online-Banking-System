package com.obs.backend.feature.bill.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class BillPaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void payingBillDebitsAccountAndCreatesLedgerRecords() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-040", "password123!");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        // 1. Create provider
        MvcResult providerResult = mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"EDC Electric\", \"category\": \"ELECTRICITY\", \"accountNumberPattern\": \"^[0-9]{8,12}$\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String providerId = JsonPath.read(providerResult.getResponse().getContentAsString(), "$.id");

        // 2. Open account & deposit funds
        MvcResult accountResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.id");

        entityManager.flush();
        entityManager
                .createNativeQuery(
                        "INSERT INTO transactions (id, account_id, type, amount, currency, description, balance_after) "
                                + "VALUES (gen_random_uuid(), :accountId, 'DEPOSIT', 500.00, 'USD', 'Initial deposit', 500.00)")
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager
                .createNativeQuery("UPDATE accounts SET balance = 500.00 WHERE id = :accountId")
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();
        entityManager.clear();

        // 3. Pay bill
        MvcResult payResult = mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "123456789",
                                          "amount": 45.50,
                                          "currency": "USD"
                                        }
                                        """
                                                .formatted(accountId, providerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(45.5))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.providerName").value("EDC Electric"))
                .andReturn();
        String paymentId = JsonPath.read(payResult.getResponse().getContentAsString(), "$.id");

        // 4. Check account balance
        mockMvc.perform(get("/accounts/" + accountId + "/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(454.5));

        // 5. Get payment receipt
        mockMvc.perform(get("/bill-payments/" + paymentId).header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.billAccountNumber").value("123456789"));

        // 6. List payment history
        mockMvc.perform(get("/bill-payments").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(paymentId)).exists());
    }

    @Test
    void invalidBillAccountNumberReturns400() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-041", "password123!");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        MvcResult providerResult = mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"PPWSA Water\", \"category\": \"WATER\", \"accountNumberPattern\": \"^[0-9]{8}$\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String providerId = JsonPath.read(providerResult.getResponse().getContentAsString(), "$.id");

        MvcResult accountResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fromAccountId": "%s",
                                          "providerId": "%s",
                                          "billAccountNumber": "abc",
                                          "amount": 20.00,
                                          "currency": "USD"
                                        }
                                        """
                                                .formatted(accountId, providerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_BILL_ACCOUNT_NUMBER"));
    }

    /**
     * A fractional amount used to reach setScale(4, RoundingMode.UNNECESSARY) in the service
     * and throw ArithmeticException out of the controller as a 500. @Digits on the request
     * makes it a field-level 400, matching CreateTransferRequest and QrPayRequest.
     */
    @Test
    void amountWithMoreThanFourDecimalPlacesIsRejected() throws Exception {
        String customerToken = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-041", "password123!");
        String adminToken = adminLoginAndGetAccessToken(mockMvc, otpSender);

        MvcResult providerResult = mockMvc.perform(
                        post("/admin/bill-providers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"EDC Precision\", \"category\": \"ELECTRICITY\", \"accountNumberPattern\": \"^[0-9]{8,12}$\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String providerId = JsonPath.read(providerResult.getResponse().getContentAsString(), "$.id");

        MvcResult accountResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.id");

        String body = """
                {
                  "fromAccountId": "%s",
                  "providerId": "%s",
                  "billAccountNumber": "123456789",
                  "amount": 10.12345,
                  "currency": "USD"
                }
                """.formatted(accountId, providerId);

        mockMvc.perform(
                        post("/bill-payments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        String recurringBody = """
                {
                  "fromAccountId": "%s",
                  "providerId": "%s",
                  "billAccountNumber": "123456789",
                  "amount": 10.12345,
                  "currency": "USD",
                  "frequency": "MONTHLY",
                  "nextPaymentDate": "2030-01-01"
                }
                """.formatted(accountId, providerId);

        mockMvc.perform(
                        post("/bill-payments/recurring")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(recurringBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }
}
