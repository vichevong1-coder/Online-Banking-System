package com.obs.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.security.jwt.JwtService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercises the real filter chain end to end against test-only endpoints, since that's the
 * actual behavior US-004/US-005 are supposed to deliver (unit-testing {@link JwtService} in
 * isolation wouldn't prove the filter and role-based authorization are wired up correctly).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityFilterChainTest.PingController.class)
class SecurityFilterChainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @Test
    void requestWithNoTokenIsRejected() throws Exception {
        mockMvc.perform(get("/security-test/ping")).andExpect(status().isForbidden());
    }

    @Test
    void requestWithValidAccessTokenIsAccepted() throws Exception {
        String token = jwtService.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));

        mockMvc.perform(get("/security-test/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void requestWithARefreshTokenAsBearerIsRejected() throws Exception {
        String token = jwtService.generateRefreshToken("customer-42");

        mockMvc.perform(get("/security-test/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpointRejectsACustomerToken() throws Exception {
        String token = jwtService.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));

        mockMvc.perform(get("/security-test/admin-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpointAcceptsAnAdminToken() throws Exception {
        String token = jwtService.generateAccessToken("staff-1", Set.of(Role.ADMIN));

        mockMvc.perform(get("/security-test/admin-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-pong"));
    }

    @RestController
    static class PingController {
        @GetMapping("/security-test/ping")
        String ping() {
            return "pong";
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/security-test/admin-only")
        String adminOnly() {
            return "admin-pong";
        }
    }
}
