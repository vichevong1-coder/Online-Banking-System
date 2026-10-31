package com.obs.backend.feature.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.admin.AdminAuthTestSupport;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Shared by feature/account and feature/statement controller tests, which only need a real
// access token as setup for authenticated endpoints — not the register/verify/login/2fa flow
// itself, which AuthenticationControllerTest already covers directly.
//
// New customers start PENDING_APPROVAL, so this helper also drives the KYC path: upload the three
// images with the signup upload token, then approve as the bootstrap admin.
public final class AuthTestSupport {

    // Only the leading bytes matter: the service sniffs the JPEG magic number.
    public static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F'};

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

        MvcResult registerResult = mockMvc.perform(
                        post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.id");

        String uploadToken = verifyRegistrationOtp(mockMvc, otpSender, phone);
        uploadAllDocuments(mockMvc, uploadToken);
        approveAsAdmin(mockMvc, otpSender, userId);

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

    /** Verifies the registration OTP and returns the KYC upload token from the response. */
    public static String verifyRegistrationOtp(MockMvc mockMvc, RecordingOtpSender otpSender, String phone)
            throws Exception {
        String registrationCode = otpSender.lastCodeFor(phone);
        MvcResult result = mockMvc.perform(
                        post("/auth/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\": \"%s\", \"code\": \"%s\"}".formatted(phone, registrationCode)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.kycUploadToken");
    }

    public static void uploadDocument(MockMvc mockMvc, String uploadToken, String type) throws Exception {
        mockMvc.perform(multipart("/auth/kyc/documents/" + type)
                        .file(new MockMultipartFile("file", "capture.jpg", "image/jpeg", JPEG_BYTES))
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk());
    }

    public static void uploadAllDocuments(MockMvc mockMvc, String uploadToken) throws Exception {
        uploadDocument(mockMvc, uploadToken, "NID_FRONT");
        uploadDocument(mockMvc, uploadToken, "NID_BACK");
        uploadDocument(mockMvc, uploadToken, "SELFIE");
    }

    public static void approveAsAdmin(MockMvc mockMvc, RecordingOtpSender otpSender, String userId)
            throws Exception {
        String adminToken = AdminAuthTestSupport.adminLoginAndGetAccessToken(mockMvc, otpSender);
        mockMvc.perform(post("/admin/kyc/" + userId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
