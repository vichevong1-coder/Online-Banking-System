package com.obs.backend.feature.card.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
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
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void cardLifecycleOperations() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-050", "password123!");

        // 1. Open account
        MvcResult accountResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.id");

        // 2. Request Card
        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\", \"pin\": \"1234\"}".formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cardType").value("DEBIT"))
                .andExpect(jsonPath("$.cardNumberMasked").exists())
                .andReturn();
        String cardId = JsonPath.read(cardResult.getResponse().getContentAsString(), "$.id");
        String maskedNumber = JsonPath.read(cardResult.getResponse().getContentAsString(), "$.cardNumberMasked");
        assertThat(maskedNumber).contains("******");

        // 3. Get Card
        mockMvc.perform(get("/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));

        // 4. Block Card
        mockMvc.perform(post("/cards/" + cardId + "/block").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        // 5. Unblock Card
        mockMvc.perform(post("/cards/" + cardId + "/unblock").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 6. Update limits & PIN
        mockMvc.perform(
                        patch("/cards/" + cardId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\": \"5678\", \"dailyLimit\": 2000.00, \"perTransactionLimit\": 800.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(2000.0))
                .andExpect(jsonPath("$.perTransactionLimit").value(800.0));

        // 7. List cards
        mockMvc.perform(get("/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(cardId)).exists());
    }

    @Test
    void accessingAnotherCustomersCardReturns404() throws Exception {
        String tokenUser1 = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-051", "password123!");
        String tokenUser2 = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-12-345-052", "password123!");

        MvcResult accountResult = mockMvc.perform(
                        post("/accounts/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountType\": \"SAVINGS\", \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.id");

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\": \"%s\"}".formatted(accountId)))
                .andExpect(status().isCreated())
                .andReturn();
        String cardId = JsonPath.read(cardResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser2))
                .andExpect(status().isNotFound());
    }
}
