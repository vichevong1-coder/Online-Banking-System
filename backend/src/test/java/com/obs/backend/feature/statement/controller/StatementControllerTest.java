package com.obs.backend.feature.statement.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
class StatementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void generatesAPdfStatementForAnOwnedAccount() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-100-001", "correct-horse");

        MvcResult openResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        MvcResult statementResult = mockMvc.perform(
                        get("/accounts/" + accountId + "/statement")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = statementResult.getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void statementIncludesSeededTransactionsWithinThePeriod() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-100-003", "correct-horse");

        MvcResult openResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        // No deposit endpoint exists yet in Sprint 2 (see sprint2-todolist.md) — seed directly,
        // same as AuthenticationControllerTest does for states with no service-level path.
        entityManager
                .createNativeQuery(
                        "INSERT INTO transactions (id, account_id, type, amount, currency, description, balance_after) "
                                + "VALUES (gen_random_uuid(), :accountId, 'DEPOSIT', 250.50, 'USD', 'Opening deposit', 250.50)")
                .setParameter("accountId", UUID.fromString(accountId))
                .executeUpdate();

        MvcResult statementResult = mockMvc.perform(
                        get("/accounts/" + accountId + "/statement")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String text;
        try (PDDocument document = Loader.loadPDF(statementResult.getResponse().getContentAsByteArray())) {
            text = new PDFTextStripper().getText(document);
        }
        assertThat(text).contains("DEPOSIT");
        assertThat(text).contains("Opening deposit");
        assertThat(text).contains("250.50");
    }

    @Test
    void statementIsRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/accounts/00000000-0000-0000-0000-000000000000/statement"))
                .andExpect(status().isForbidden());
    }

    @Test
    void statementForAnUnknownAccountReturnsNotFound() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-11-100-002", "correct-horse");

        mockMvc.perform(
                        get("/accounts/00000000-0000-0000-0000-000000000000/statement")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
