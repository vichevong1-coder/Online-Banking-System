package com.obs.backend.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obs.backend.feature.auth.repository.LoginAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
class LoginAttemptTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LoginAttemptRepository loginAttemptRepository;

    @Test
    void failedLoginRecordsAttempt() throws Exception {
        long beforeCount = loginAttemptRepository.countBySuccessFalse();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nonexistent@obs.local\", \"password\": \"WrongPassword!\"}"))
                .andExpect(status().isUnauthorized());

        long afterCount = loginAttemptRepository.countBySuccessFalse();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }
}
