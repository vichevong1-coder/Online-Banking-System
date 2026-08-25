package com.obs.backend.feature.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for POST /auth/register covering XSS/injection payloads,
 * password boundary validation, field length limits, mass assignment prevention,
 * and invalid enum/date inputs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private static final String VALID_BODY_TEMPLATE =
            """
            {
              "firstName": "%s",
              "lastName": "%s",
              "password": "%s",
              "nidNumber": "123456789",
              "nidExpiryDate": "2030-01-01",
              "dateOfBirth": "1990-01-01",
              "gender": "FEMALE",
              "phone": "%s"
            }
            """;

    // --- XSS / Injection ---

    @Test
    void xssScriptTagInFirstNameIsStoredLiterally() throws Exception {
        String phone = "+855-91-001-001";
        String xssPayload = "<script>alert(1)</script>";
        String body = VALID_BODY_TEMPLATE.formatted(xssPayload, "Doe", "correct-horse", phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(xssPayload));
    }

    @Test
    void xssScriptTagInLastNameIsStoredLiterally() throws Exception {
        String phone = "+855-91-001-002";
        String xssPayload = "<img onerror=alert(1) src=x>";
        String body = VALID_BODY_TEMPLATE.formatted("Jane", xssPayload, "correct-horse", phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastName").value(xssPayload));
    }

    @Test
    void sqlInjectionInFirstNameIsStoredLiterally() throws Exception {
        String phone = "+855-91-001-003";
        String sqlPayload = "' OR '1'='1";
        String body = VALID_BODY_TEMPLATE.formatted(sqlPayload, "Doe", "correct-horse", phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(sqlPayload));
    }

    // --- Password boundary validation ---

    @Test
    void passwordTooShortIsRejected() throws Exception {
        // 7 chars, min is 8
        String body = VALID_BODY_TEMPLATE.formatted("Jane", "Doe", "short12", "+855-91-001-004");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void passwordTooLongIsRejected() throws Exception {
        // 101 chars, max is 100
        String longPassword = "A".repeat(101);
        String body = VALID_BODY_TEMPLATE.formatted("Jane", "Doe", longPassword, "+855-91-001-005");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void blankPasswordIsRejected() throws Exception {
        String body = VALID_BODY_TEMPLATE.formatted("Jane", "Doe", "   ", "+855-91-001-006");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // --- Field length limits ---

    @Test
    void firstNameExceeding255CharsIsRejected() throws Exception {
        String longName = "A".repeat(256);
        String body = VALID_BODY_TEMPLATE.formatted(longName, "Doe", "correct-horse", "+855-91-001-007");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void lastNameExceeding255CharsIsRejected() throws Exception {
        String longName = "A".repeat(256);
        String body = VALID_BODY_TEMPLATE.formatted("Jane", longName, "correct-horse", "+855-91-001-008");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists());
    }

    // --- Mass assignment prevention ---

    @Test
    void sendingRoleAdminInRegistrationIsIgnored() throws Exception {
        String phone = "+855-91-001-009";
        String body =
                """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "password": "correct-horse",
                  "nidNumber": "123456789",
                  "nidExpiryDate": "2030-01-01",
                  "dateOfBirth": "1990-01-01",
                  "gender": "FEMALE",
                  "phone": "%s",
                  "role": "ADMIN"
                }
                """.formatted(phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        User saved = userRepository.findByPhone(phone).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void sendingPhoneVerifiedTrueInRegistrationIsIgnored() throws Exception {
        String phone = "+855-91-001-010";
        String body =
                """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "password": "correct-horse",
                  "nidNumber": "123456789",
                  "nidExpiryDate": "2030-01-01",
                  "dateOfBirth": "1990-01-01",
                  "gender": "FEMALE",
                  "phone": "%s",
                  "phoneVerified": true
                }
                """.formatted(phone);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        User saved = userRepository.findByPhone(phone).orElseThrow();
        assertThat(saved.isPhoneVerified()).isFalse();
    }

    // --- Date and enum validation ---

    @Test
    void futureDateOfBirthIsRejected() throws Exception {
        String body =
                """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "password": "correct-horse",
                  "nidNumber": "123456789",
                  "nidExpiryDate": "2030-01-01",
                  "dateOfBirth": "2099-01-01",
                  "gender": "FEMALE",
                  "phone": "+855-91-001-011"
                }
                """;

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidGenderEnumIsRejected() throws Exception {
        String body =
                """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "password": "correct-horse",
                  "nidNumber": "123456789",
                  "nidExpiryDate": "2030-01-01",
                  "dateOfBirth": "1990-01-01",
                  "gender": "UNKNOWN",
                  "phone": "+855-91-001-012"
                }
                """;

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
