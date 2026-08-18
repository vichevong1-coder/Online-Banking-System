package com.obs.backend.feature.beneficiary.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-029 / US-030.
 *
 * <p>A beneficiary needs no funded account and no transfer behind it — it is an
 * address book entry — so setup here is just a token per customer. Ownership is
 * the part worth testing hardest: every single-row endpoint has to answer 404,
 * not 403, for another customer's id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class BeneficiaryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;

    @Test
    void addingABeneficiaryReturnsItAndPutsItOnTheCallersList() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-001", "correct-horse");

        mockMvc.perform(createRequest(token, "Landlord", "ABCDKHPP", "9988776655"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.displayName").value("Landlord"))
                .andExpect(jsonPath("$.bankCode").value("ABCDKHPP"))
                .andExpect(jsonPath("$.accountNumber").value("9988776655"))
                // US-031's flag exists from the start and defaults to false.
                .andExpect(jsonPath("$.favorite").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Landlord"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void aBeneficiaryWithAMalformedDestinationFailsValidation() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-002", "correct-horse");

        // Same shapes as CreateExternalTransferRequest: bank code too short,
        // account number too short, name blank.
        mockMvc.perform(createRequest(token, "", "ab", "12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.displayName").exists())
                .andExpect(jsonPath("$.fieldErrors.bankCode").exists())
                .andExpect(jsonPath("$.fieldErrors.accountNumber").exists());
    }

    @Test
    void savingTheSameDestinationTwiceIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-003", "correct-horse");

        mockMvc.perform(createRequest(token, "Landlord", "ABCDKHPP", "9988776655"))
                .andExpect(status().isCreated());

        // A different display name for the same account is still the same payee.
        mockMvc.perform(createRequest(token, "Landlord (new number)", "ABCDKHPP", "9988776655"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_ALREADY_EXISTS"));
    }

    @Test
    void twoCustomersMaySaveTheSameDestination() throws Exception {
        // The uniqueness rule is per user — a shared landlord is not a duplicate.
        String first = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-004", "correct-horse");
        String second = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-005", "correct-horse");

        mockMvc.perform(createRequest(first, "Landlord", "ABCDKHPP", "9988776655"))
                .andExpect(status().isCreated());
        mockMvc.perform(createRequest(second, "Landlord", "ABCDKHPP", "9988776655"))
                .andExpect(status().isCreated());
    }

    @Test
    void theListShowsOnlyTheCallersOwnBeneficiaries() throws Exception {
        String owner = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-006", "correct-horse");
        String other = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-29-000-007", "correct-horse");

        create(owner, "Mine", "ABCDKHPP", "1111111111");
        create(other, "Theirs", "WXYZKHPP", "2222222222");

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Mine"));
        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Theirs"));
    }

    @Test
    void patchingOnlyTheNameLeavesEveryOtherFieldAlone() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-001", "correct-horse");
        String id = create(token, "Landlord", "ABCDKHPP", "9988776655");

        // The point of PATCH: a body carrying one field must not null out the rest.
        mockMvc.perform(patchRequest(token, id, "{\"displayName\": \"Landlord (Phnom Penh)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.displayName").value("Landlord (Phnom Penh)"))
                .andExpect(jsonPath("$.bankCode").value("ABCDKHPP"))
                .andExpect(jsonPath("$.accountNumber").value("9988776655"))
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void patchingTheDestinationAndTheFavoriteFlagKeepsTheName() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-002", "correct-horse");
        String id = create(token, "Landlord", "ABCDKHPP", "9988776655");

        // US-031's flag is set through the edit endpoint and nowhere else.
        mockMvc.perform(patchRequest(token, id, "{\"accountNumber\": \"5544332211\", \"favorite\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Landlord"))
                .andExpect(jsonPath("$.bankCode").value("ABCDKHPP"))
                .andExpect(jsonPath("$.accountNumber").value("5544332211"))
                .andExpect(jsonPath("$.favorite").value(true));

        // And it survives a later edit that does not mention it.
        mockMvc.perform(patchRequest(token, id, "{\"displayName\": \"Landlord renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.accountNumber").value("5544332211"));
    }

    @Test
    void anEmptyPatchBodyChangesNothing() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-003", "correct-horse");
        String id = create(token, "Landlord", "ABCDKHPP", "9988776655");

        mockMvc.perform(patchRequest(token, id, "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Landlord"))
                .andExpect(jsonPath("$.bankCode").value("ABCDKHPP"))
                .andExpect(jsonPath("$.accountNumber").value("9988776655"));
    }

    @Test
    void patchingOntoADestinationTheCallerAlreadySavedIsRejected() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-004", "correct-horse");
        create(token, "Landlord", "ABCDKHPP", "9988776655");
        String second = create(token, "Sister", "ABCDKHPP", "1231231234");

        mockMvc.perform(patchRequest(token, second, "{\"accountNumber\": \"9988776655\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_ALREADY_EXISTS"));
    }

    @Test
    void aPatchWithAMalformedFieldFailsValidation() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-005", "correct-horse");
        String id = create(token, "Landlord", "ABCDKHPP", "9988776655");

        mockMvc.perform(patchRequest(token, id, "{\"displayName\": \"\", \"bankCode\": \"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.displayName").exists())
                .andExpect(jsonPath("$.fieldErrors.bankCode").exists());
    }

    @Test
    void deletingABeneficiaryRemovesItFromTheList() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-006", "correct-horse");
        String id = create(token, "Landlord", "ABCDKHPP", "9988776655");

        mockMvc.perform(delete("/beneficiaries/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(0));

        // Gone, so a second delete is a 404 like any other unknown id.
        mockMvc.perform(delete("/beneficiaries/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_NOT_FOUND"));
    }

    @Test
    void patchingAnotherCustomersBeneficiaryReturnsNotFoundAndChangesNothing() throws Exception {
        String owner = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-007", "correct-horse");
        String id = create(owner, "Landlord", "ABCDKHPP", "9988776655");

        String intruder = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-008", "correct-horse");

        // 404, not 403 — an id must not reveal that it belongs to somebody else.
        mockMvc.perform(patchRequest(intruder, id, "{\"displayName\": \"Stolen\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_NOT_FOUND"));

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
                .andExpect(jsonPath("$.content[0].displayName").value("Landlord"));
    }

    @Test
    void deletingAnotherCustomersBeneficiaryReturnsNotFoundAndLeavesItInPlace() throws Exception {
        String owner = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-009", "correct-horse");
        String id = create(owner, "Landlord", "ABCDKHPP", "9988776655");

        String intruder = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-010", "correct-horse");

        mockMvc.perform(delete("/beneficiaries/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + intruder))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_NOT_FOUND"));

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void anUnknownBeneficiaryIdReturnsNotFound() throws Exception {
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-30-000-011", "correct-horse");

        mockMvc.perform(patchRequest(token, UUID.randomUUID().toString(), "{\"favorite\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BENEFICIARY_NOT_FOUND"));
    }

    @Test
    void theBeneficiaryEndpointsAreRejectedWithoutAToken() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(
                        post("/beneficiaries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\": \"Landlord\", \"bankCode\": \"ABCDKHPP\","
                                        + " \"accountNumber\": \"9988776655\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/beneficiaries")).andExpect(status().isForbidden());
        mockMvc.perform(
                        patch("/beneficiaries/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"displayName\": \"Landlord\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/beneficiaries/" + id)).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder createRequest(
            String token, String displayName, String bankCode, String accountNumber) {
        String body = "{\"displayName\": \"%s\", \"bankCode\": \"%s\", \"accountNumber\": \"%s\"}"
                .formatted(displayName, bankCode, accountNumber);
        return post("/beneficiaries")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder patchRequest(String token, String beneficiaryId, String body) {
        return patch("/beneficiaries/" + beneficiaryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String create(String token, String displayName, String bankCode, String accountNumber) throws Exception {
        MvcResult result = mockMvc.perform(createRequest(token, displayName, bankCode, accountNumber))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
