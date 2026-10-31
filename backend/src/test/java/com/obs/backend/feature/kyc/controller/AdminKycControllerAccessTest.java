package com.obs.backend.feature.kyc.controller;

import static com.obs.backend.feature.account.AuthTestSupport.JPEG_BYTES;
import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static com.obs.backend.feature.account.AuthTestSupport.uploadAllDocuments;
import static com.obs.backend.feature.account.AuthTestSupport.verifyRegistrationOtp;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.adminLoginAndGetAccessToken;
import static com.obs.backend.feature.admin.AdminAuthTestSupport.createTellerAndGetAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatus;
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

/**
 * Access control tests for /admin/kyc/** endpoints.
 * Verifies that:
 * - TELLER and ADMIN can access pending queue, application details, document images, approve, and reject.
 * - CUSTOMER tokens are forbidden (403) from all review endpoints.
 * - Anonymous requests are unauthorized (401).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class AdminKycControllerAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @Autowired private UserRepository userRepository;

    private UUID createPendingCustomerWithDocuments(String phone) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Kyc",
                                  "lastName": "Applicant",
                                  "password": "Password123!",
                                  "nidNumber": "112233445",
                                  "nidExpiryDate": "2032-01-01",
                                  "dateOfBirth": "1995-05-15",
                                  "gender": "FEMALE",
                                  "phone": "%s"
                                }
                                """.formatted(phone)))
                .andExpect(status().isCreated());

        String uploadToken = verifyRegistrationOtp(mockMvc, otpSender, phone);
        uploadAllDocuments(mockMvc, uploadToken);
        return userRepository.findByPhone(phone).orElseThrow().getId();
    }

    private String getTellerToken() throws Exception {
        return createTellerAndGetAccessToken(
                mockMvc, otpSender, "teller.access.test@obs.local", "+855-16-777-111", "Password123!");
    }

    private String getAdminToken() throws Exception {
        return adminLoginAndGetAccessToken(mockMvc, otpSender);
    }

    private String getCustomerToken() throws Exception {
        return registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-13-888-222", "Customer123!");
    }

    // ------------------------------------------------------------------- CUSTOMER FORBIDDEN (403)

    @Test
    void customerTokenIsForbiddenFromPendingList() throws Exception {
        String customerToken = getCustomerToken();

        mockMvc.perform(get("/admin/kyc/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenIsForbiddenFromApplicationDetail() throws Exception {
        String customerToken = getCustomerToken();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/admin/kyc/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenIsForbiddenFromDocumentImage() throws Exception {
        String customerToken = getCustomerToken();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/admin/kyc/" + userId + "/documents/NID_FRONT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/kyc/" + userId + "/documents/NID_FRONT/image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenIsForbiddenFromApprove() throws Exception {
        String customerToken = getCustomerToken();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/admin/kyc/" + userId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenIsForbiddenFromReject() throws Exception {
        String customerToken = getCustomerToken();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/admin/kyc/" + userId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Declined\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------- ANONYMOUS UNAUTHORIZED (401)

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/admin/kyc/pending")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/kyc/" + userId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/kyc/" + userId + "/documents/NID_FRONT")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/admin/kyc/" + userId + "/approve")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/admin/kyc/" + userId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Declined\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------- TELLER ALLOWED (200)

    @Test
    void tellerCanListPendingKycApplications() throws Exception {
        String tellerToken = getTellerToken();
        createPendingCustomerWithDocuments("+855-33-777-001");

        mockMvc.perform(get("/admin/kyc/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.phone=='+855-33-777-001')]").isNotEmpty());
    }

    @Test
    void tellerCanViewApplicationDetailAndDocumentImages() throws Exception {
        String tellerToken = getTellerToken();
        UUID customerId = createPendingCustomerWithDocuments("+855-33-777-002");

        mockMvc.perform(get("/admin/kyc/" + customerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(customerId.toString()))
                .andExpect(jsonPath("$.phone").value("+855-33-777-002"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.documents.length()").value(3));

        mockMvc.perform(get("/admin/kyc/" + customerId + "/documents/NID_FRONT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG_BYTES));

        mockMvc.perform(get("/admin/kyc/" + customerId + "/documents/NID_FRONT/image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG_BYTES));
    }

    @Test
    void tellerCanApproveKycApplication() throws Exception {
        String tellerToken = getTellerToken();
        UUID customerId = createPendingCustomerWithDocuments("+855-33-777-003");

        mockMvc.perform(post("/admin/kyc/" + customerId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User updatedUser = userRepository.findById(customerId).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(updatedUser.getReviewedBy()).isNotNull();
        assertThat(updatedUser.getReviewedAt()).isNotNull();
    }

    @Test
    void tellerCanRejectKycApplication() throws Exception {
        String tellerToken = getTellerToken();
        UUID customerId = createPendingCustomerWithDocuments("+855-33-777-004");

        mockMvc.perform(post("/admin/kyc/" + customerId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"ID document is expired\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("ID document is expired"));

        User updatedUser = userRepository.findById(customerId).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(AccountStatus.REJECTED);
        assertThat(updatedUser.getRejectionReason()).isEqualTo("ID document is expired");
        assertThat(updatedUser.getReviewedBy()).isNotNull();
        assertThat(updatedUser.getReviewedAt()).isNotNull();
    }

    // ------------------------------------------------------------------- ADMIN ALLOWED (200)

    @Test
    void adminCanAccessAllKycEndpoints() throws Exception {
        String adminToken = getAdminToken();
        UUID customerId = createPendingCustomerWithDocuments("+855-33-777-005");

        mockMvc.perform(get("/admin/kyc/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/kyc/" + customerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(customerId.toString()));

        mockMvc.perform(get("/admin/kyc/" + customerId + "/documents/NID_FRONT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(JPEG_BYTES));

        mockMvc.perform(post("/admin/kyc/" + customerId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
