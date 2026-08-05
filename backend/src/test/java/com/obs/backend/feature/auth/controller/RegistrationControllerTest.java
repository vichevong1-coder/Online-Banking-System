package com.obs.backend.feature.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real end-to-end coverage of POST /auth/register: no Authorization header is sent, proving
 * the SecurityConfig permit rule works, not just that the service logic is correct.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String VALID_BODY =
            """
            {
              "firstName": "Jane",
              "lastName": "Doe",
              "password": "correct-horse",
              "nidNumber": "123456789",
              "nidExpiryDate": "2030-01-01",
              "dateOfBirth": "1990-01-01",
              "gender": "FEMALE",
              "phone": "+855-12-345-678"
            }
            """;

    @Test
    void registersANewCustomerWithoutAnAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phone").value("+855-12-345-678"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void rejectsADuplicatePhoneWithConflict() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        String duplicatePhoneBody = VALID_BODY.replace("\"nidNumber\": \"123456789\"", "\"nidNumber\": \"987654321\"");
        mockMvc.perform(
                        post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(duplicatePhoneBody))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAMissingRequiredField() throws Exception {
        String missingFirstName =
                """
                {
                  "lastName": "Doe",
                  "password": "correct-horse",
                  "nidNumber": "123456789",
                  "nidExpiryDate": "2030-01-01",
                  "dateOfBirth": "1990-01-01",
                  "gender": "FEMALE",
                  "phone": "+855-12-345-678"
                }
                """;

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(missingFirstName))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnExpiredNid() throws Exception {
        String expiredNidBody = VALID_BODY.replace("\"nidExpiryDate\": \"2030-01-01\"", "\"nidExpiryDate\": \"2020-01-01\"");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(expiredNidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void persistsTheExpectedDefaultsForANewCustomer() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        User saved = userRepository.findByPhone("+855-12-345-678").orElseThrow();

        assertThat(saved.getPasswordHash()).isNotEqualTo("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isPhoneVerified()).isFalse();
    }
}
