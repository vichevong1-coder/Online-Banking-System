package com.obs.backend.feature.notification.controller;

import static com.obs.backend.feature.account.AuthTestSupport.registerVerifyLoginAndGetAccessToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig;
import com.obs.backend.feature.auth.RecordingOtpSenderConfig.RecordingOtpSender;
import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.service.NotificationService;
import com.obs.backend.feature.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security tests for notification endpoints: cross-user IDOR on listing
 * and markAsRead, unauthenticated access, non-existent notification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordingOtpSenderConfig.class)
@Transactional
class NotificationSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RecordingOtpSender otpSender;
    @Autowired private NotificationService notificationService;
    @Autowired private UserRepository userRepository;

    @Test
    void listingNotificationsDoesNotLeakAnotherUsersData() throws Exception {
        String userAToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-002-001", "correct-horse");
        UUID userAId = userRepository.findByPhone("+855-92-002-001").orElseThrow().getId();

        String userBToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-002-002", "correct-horse");

        notificationService.sendNotification(
                userAId, "Secret Alert", "Confidential data", NotificationType.BALANCE_ALERT);

        // User B must NOT see User A's notifications.
        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // User A sees their own.
        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Secret Alert"));
    }

    @Test
    void markingAnotherUsersNotificationAsReadReturns404() throws Exception {
        // 404 rather than 403, the Sprint 2 rule: the lookup is scoped to the caller, so
        // another customer's notification is indistinguishable from one that does not exist
        // and an id cannot be used to probe ownership. The owner's copy stays unread.
        String userAToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-002-003", "correct-horse");
        UUID userAId = userRepository.findByPhone("+855-92-002-003").orElseThrow().getId();

        String userBToken =
                registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-002-004", "correct-horse");

        notificationService.sendNotification(
                userAId, "Private", "Private notification", NotificationType.BALANCE_ALERT);

        // Get notification ID via User A
        MvcResult result = mockMvc.perform(
                        get("/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();
        String notificationId = JsonPath.read(result.getResponse().getContentAsString(), "$.content[0].id");

        // User B tries to mark User A's notification as read.
        mockMvc.perform(
                        patch("/notifications/" + notificationId + "/read")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // Verify it remains unread for User A — the cross-user attempt must not mutate it.
        mockMvc.perform(
                        get("/notifications/unread-count")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void markingNonExistentNotificationReturns404() throws Exception {
        // An unknown id is a 404, not a silent 204: reporting success for a write that did
        // not happen is what this used to do, and it made a no-op indistinguishable from a
        // real update.
        String token = registerVerifyLoginAndGetAccessToken(mockMvc, otpSender, "+855-92-002-005", "correct-horse");

        mockMvc.perform(
                        patch("/notifications/" + UUID.randomUUID() + "/read")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void notificationEndpointsAreRejectedWithoutAToken() throws Exception {
        mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/notifications/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isUnauthorized());
    }
}
