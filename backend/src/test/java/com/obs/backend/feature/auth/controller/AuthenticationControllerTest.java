package com.obs.backend.feature.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises login through the seeded bootstrap admin (V2 migration) rather than provisioning an
 * admin through application code — creating admin accounts is deliberately out of scope for
 * Sprint 1 (deferred to US-047).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AuthenticationControllerTest {

    private static final String ADMIN_EMAIL = "admin@obs.local";
    private static final String ADMIN_PASSWORD = "ChangeMe123!";
    private static final String ADMIN_PHONE = "+855000000000";

    private static final String CUSTOMER_PHONE = "+855-22-333-444";
    private static final String CUSTOMER_PASSWORD = "correct-horse";
    private static final String REGISTER_BODY =
            """
            {
              "firstName": "Jane",
              "lastName": "Doe",
              "password": "%s",
              "nidNumber": "123456789",
              "nidExpiryDate": "2030-01-01",
              "dateOfBirth": "1990-01-01",
              "gender": "FEMALE",
              "phone": "%s"
            }
            """
                    .formatted(CUSTOMER_PASSWORD, CUSTOMER_PHONE);

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void adminLogsInWithEmailCompletesTwoFactorThenRefreshes() throws Exception {
        String challengeToken = login(ADMIN_EMAIL, null, ADMIN_PASSWORD);
        String code = otpSender.lastCodeFor(ADMIN_PHONE);
        assertThat(code).isNotNull();

        MvcResult verifyResult =
                mockMvc.perform(
                                post("/auth/2fa/verify")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                        .formatted(challengeToken, code)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").exists())
                        .andExpect(jsonPath("$.refreshToken").exists())
                        .andExpect(jsonPath("$.role").value("ADMIN"))
                        .andReturn();

        String refreshToken =
                JsonPath.read(verifyResult.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(
                        post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void resendIssuesAFreshCodeAndInvalidatesTheOldOne() throws Exception {
        String firstChallenge = login(ADMIN_EMAIL, null, ADMIN_PASSWORD);
        String firstCode = otpSender.lastCodeFor(ADMIN_PHONE);

        MvcResult resendResult =
                mockMvc.perform(
                                post("/auth/2fa/resend")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"challengeToken\": \"%s\"}".formatted(firstChallenge)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.challengeToken").exists())
                        .andReturn();
        String newChallenge = JsonPath.read(resendResult.getResponse().getContentAsString(), "$.challengeToken");
        String newCode = otpSender.lastCodeFor(ADMIN_PHONE);
        assertThat(newCode).isNotEqualTo(firstCode);

        // The pre-resend code no longer verifies, against either challenge token.
        mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(newChallenge, firstCode)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(newChallenge, newCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void resendRejectsAGarbageChallengeToken() throws Exception {
        mockMvc.perform(
                        post("/auth/2fa/resend")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"challengeToken\": \"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPasswordWithUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\": \"%s\", \"password\": \"wrong-password\"}"
                                                .formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBothEmailAndPhoneWithBadRequest() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\": \"%s\", \"phone\": \"%s\", \"password\": \"%s\"}"
                                                .formatted(ADMIN_EMAIL, ADMIN_PHONE, ADMIN_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNeitherEmailNorPhoneWithBadRequest() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\": \"%s\"}".formatted(ADMIN_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void twoFactorVerifyRejectsAWrongCode() throws Exception {
        String challengeToken = login(ADMIN_EMAIL, null, ADMIN_PASSWORD);

        mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"000000\"}"
                                                .formatted(challengeToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshRejectsAGarbageToken() throws Exception {
        mockMvc.perform(
                        post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsASuspendedAccount() throws Exception {
        entityManager
                .createNativeQuery("UPDATE users SET status = 'SUSPENDED' WHERE email = :email")
                .setParameter("email", ADMIN_EMAIL)
                .executeUpdate();

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\": \"%s\", \"password\": \"%s\"}"
                                                .formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void customerCannotLoginBeforePhoneIsVerified() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"phone\": \"%s\", \"password\": \"%s\"}"
                                                .formatted(CUSTOMER_PHONE, CUSTOMER_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PHONE_NOT_VERIFIED"));
    }

    @Test
    void customerLogsInByPhoneAfterVerification() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());
        String registrationCode = otpSender.lastCodeFor(CUSTOMER_PHONE);
        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"phone\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(CUSTOMER_PHONE, registrationCode)))
                .andExpect(status().isNoContent());

        String challengeToken = login(null, CUSTOMER_PHONE, CUSTOMER_PASSWORD);
        String loginCode = otpSender.lastCodeFor(CUSTOMER_PHONE);
        assertThat(loginCode).isNotNull();

        mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(challengeToken, loginCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    private String login(String email, String phone, String password) throws Exception {
        StringBuilder body = new StringBuilder("{");
        if (email != null) {
            body.append("\"email\": \"").append(email).append("\",");
        }
        if (phone != null) {
            body.append("\"phone\": \"").append(phone).append("\",");
        }
        body.append("\"password\": \"").append(password).append("\"}");

        MvcResult result =
                mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.challengeToken").exists())
                        .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.challengeToken");
    }
}
