package com.obs.backend.feature.devtools;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DevSmsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DevSmsOutbox outbox;

    @Test
    void inboxPageIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/dev/sms"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SMS inbox")));
    }

    @Test
    void jsonFeedListsNewestMessageFirst() throws Exception {
        outbox.add("+855 10-000-001", "first message 111111");
        outbox.add("+855 10-000-002", "second message 222222");

        mockMvc.perform(get("/dev/sms.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").value("+855 10-000-002"))
                .andExpect(jsonPath("$[0].message").value("second message 222222"));
    }

    @Test
    void otherEndpointsStayProtected() throws Exception {
        mockMvc.perform(get("/accounts")).andExpect(status().is4xxClientError());
    }
}
