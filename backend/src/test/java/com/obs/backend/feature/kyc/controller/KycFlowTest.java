package com.obs.backend.feature.kyc.controller;

import static com.obs.backend.feature.account.AuthTestSupport.JPEG_BYTES;
import static com.obs.backend.feature.account.AuthTestSupport.uploadAllDocuments;
import static com.obs.backend.feature.account.AuthTestSupport.uploadDocument;
import static com.obs.backend.feature.account.AuthTestSupport.verifyRegistrationOtp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.admin.AdminAuthTestSupport;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class KycFlowTest {

    private static final String PHONE = "+855-33-444-555";
    private static final String PASSWORD = "correct-horse";
    private static final String REGISTER_BODY =
            """
            {
              "firstName": "Sok",
              "lastName": "Dara",
              "password": "%s",
              "nidNumber": "987654321",
              "nidExpiryDate": "2030-01-01",
              "dateOfBirth": "1992-02-02",
              "gender": "MALE",
              "phone": "%s"
            }
            """
                    .formatted(PASSWORD, PHONE);

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @Autowired private UserRepository userRepository;

    private String registerAndGetUploadToken() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());
        return verifyRegistrationOtp(mockMvc, otpSender, PHONE);
    }

    private String userId() {
        return userRepository.findByPhone(PHONE).orElseThrow().getId().toString();
    }

    private String adminToken() throws Exception {
        return AdminAuthTestSupport.adminLoginAndGetAccessToken(mockMvc, otpSender);
    }

    private void expectLoginError(int httpStatus, String code) throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"%s\"}".formatted(PHONE, PASSWORD)))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error").value(code));
    }

    @Test
    void uploadReportsWhichImagesAreStillMissing() throws Exception {
        String token = registerAndGetUploadToken();

        mockMvc.perform(multipart("/auth/kyc/documents/NID_FRONT")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing.length()").value(2));
    }

    @Test
    void uploadWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/auth/kyc/documents/SELFIE")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAccessTokenCannotBeUsedToUpload() throws Exception {
        String token = adminToken();
        mockMvc.perform(multipart("/auth/kyc/documents/SELFIE")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAFileThatIsNotAnImage() throws Exception {
        String token = registerAndGetUploadToken();
        mockMvc.perform(multipart("/auth/kyc/documents/SELFIE")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "not an image".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_KYC_DOCUMENT"));
    }

    @Test
    void pendingCustomerCannotLogInAndIsToldWhy() throws Exception {
        registerAndGetUploadToken();
        expectLoginError(403, "ACCOUNT_PENDING_APPROVAL");
    }

    @Test
    void applicationOnlyAppearsInTheQueueOnceAllImagesAreUploaded() throws Exception {
        String token = registerAndGetUploadToken();
        String admin = adminToken();
        uploadDocument(mockMvc, token, "NID_FRONT");

        mockMvc.perform(get("/admin/kyc/pending").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone=='%s')]".formatted(PHONE)).isEmpty());

        uploadDocument(mockMvc, token, "NID_BACK");
        uploadDocument(mockMvc, token, "SELFIE");

        mockMvc.perform(get("/admin/kyc/pending").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone=='%s')]".formatted(PHONE)).isNotEmpty());
    }

    @Test
    void reviewerGetsTheDecryptedImageBack() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);

        MvcResult result = mockMvc.perform(get("/admin/kyc/" + userId() + "/documents/SELFIE/image")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(JPEG_BYTES);
    }

    @Test
    void approvalActivatesTheAccountSoItCanLogIn() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);

        mockMvc.perform(post("/admin/kyc/" + userId() + "/approve").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(userRepository.findByPhone(PHONE).orElseThrow().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\": \"%s\", \"password\": \"%s\"}".formatted(PHONE, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void cannotApproveWhileImagesAreMissing() throws Exception {
        String token = registerAndGetUploadToken();
        uploadDocument(mockMvc, token, "SELFIE");

        mockMvc.perform(post("/admin/kyc/" + userId() + "/approve").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("KYC_DOCUMENTS_INCOMPLETE"));
    }

    @Test
    void rejectionRecordsTheReasonAndBlocksLogin() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);

        mockMvc.perform(post("/admin/kyc/" + userId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Selfie is blurry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Selfie is blurry"));

        User saved = userRepository.findByPhone(PHONE).orElseThrow();
        assertThat(saved.getRejectionReason()).isEqualTo("Selfie is blurry");
        assertThat(saved.getReviewedBy()).isNotNull();
        expectLoginError(403, "ACCOUNT_REJECTED");
    }

    @Test
    void rejectRequiresAReason() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);

        mockMvc.perform(post("/admin/kyc/" + userId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anApprovedApplicationCannotBeReviewedAgain() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);
        String admin = adminToken();

        mockMvc.perform(post("/admin/kyc/" + userId() + "/approve").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/kyc/" + userId() + "/approve").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("KYC_NOT_PENDING"));
    }

    @Test
    void uploadIsRefusedOnceTheApplicationIsApproved() throws Exception {
        String token = registerAndGetUploadToken();
        uploadAllDocuments(mockMvc, token);
        mockMvc.perform(post("/admin/kyc/" + userId() + "/approve").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/auth/kyc/documents/SELFIE")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("KYC_UPLOAD_NOT_ALLOWED"));
    }

    @Test
    void genericStatusEndpointCannotSkipReview() throws Exception {
        registerAndGetUploadToken();

        mockMvc.perform(patch("/admin/customers/" + userId() + "/status")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("KYC_REVIEW_REQUIRED"));
    }

    @Test
    void customerAccessTokenCannotReachTheReviewEndpoints() throws Exception {
        String customerToken = com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken(
                mockMvc, otpSender, "+855-33-444-556", PASSWORD);

        mockMvc.perform(get("/admin/kyc/pending").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReviewRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/kyc/pending")).andExpect(status().isUnauthorized());
    }
}
