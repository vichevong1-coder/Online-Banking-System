package com.obs.backend.feature.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Shared by feature/account and feature/statement controller tests, which only need a real
// access token as setup for authenticated endpoints — not the register/verify/login/2fa flow
// itself, which AuthenticationControllerTest already covers directly.
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static String registerVerifyLoginAndGetAccessToken(
            MockMvc mockMvc, RecordingOtpSender otpSender, String phone, String password) throws Exception {
        String registerBody =
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
                        .formatted(password, phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        String registrationCode = otpSender.lastCodeFor(phone);
        mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"%s\"}".formatted(phone, registrationCode)))
                .andExpect(status().isNoContent());

        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"password\": \"%s\"}".formatted(phone, password)))
                .andExpect(status().isOk())
                .andReturn();
        String challengeToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.challengeToken");
        String loginCode = otpSender.lastCodeFor(phone);

        MvcResult verifyResult = mockMvc.perform(
                        post("/auth/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"challengeToken\": \"%s\", \"code\": \"%s\"}"
                                                .formatted(challengeToken, loginCode)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(verifyResult.getResponse().getContentAsString(), "$.accessToken");
    }
}
