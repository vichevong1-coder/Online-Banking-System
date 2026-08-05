package com.obs.backend.feature.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import com.obs.backend.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class OtpControllerTest {

    private static final String PHONE = "+855-11-222-333";
    private static final String REGISTER_BODY =
            """
            {
              "firstName": "Jane",
              "lastName": "Doe",
              "password": "correct-horse",
              "nidNumber": "123456789",
              "nidExpiryDate": "2030-01-01",
              "dateOfBirth": "1990-01-01",
              "gender": "FEMALE",
              "phone": "%s"
            }
            """
                    .formatted(PHONE);

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @Autowired private UserRepository userRepository;

    @Test
    void registrationSendsACodeThatVerifySucceedsWith() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        String code = otpSender.lastCodeFor(PHONE);
        assertThat(code).isNotNull();
        assertThat(userRepository.findByPhone(PHONE).orElseThrow().isPhoneVerified()).isFalse();

        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"%s\"}".formatted(PHONE, code)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByPhone(PHONE).orElseThrow().isPhoneVerified()).isTrue();
    }

    @Test
    void rejectsAWrongCodeWithBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"000000\"}".formatted(PHONE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendIssuesAFreshCodeThatVerifySucceedsWith() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());
        String firstCode = otpSender.lastCodeFor(PHONE);

        mockMvc.perform(
                        post("/auth/otp/resend")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\"}".formatted(PHONE)))
                .andExpect(status().isNoContent());
        String resentCode = otpSender.lastCodeFor(PHONE);

        // Old code must no longer verify — resend invalidates it, not just adds a new one.
        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"%s\"}".formatted(PHONE, firstCode)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"%s\"}".formatted(PHONE, resentCode)))
                .andExpect(status().isNoContent());
    }

    @Test
    void verifyForAnUnknownPhoneReturnsNotFound() throws Exception {
        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"+855-00-000-000\", \"code\": \"123456\"}"))
                .andExpect(status().isNotFound());
    }
}
