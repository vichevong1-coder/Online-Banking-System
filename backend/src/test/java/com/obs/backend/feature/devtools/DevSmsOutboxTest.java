package com.obs.backend.feature.devtools;

import static org.assertj.core.api.Assertions.assertThat;

import com.obs.backend.feature.devtools.DevSmsOutbox.DevSms;
import java.util.List;
import org.junit.jupiter.api.Test;

class DevSmsOutboxTest {

    @Test
    void returnsNewestFirst() {
        DevSmsOutbox outbox = new DevSmsOutbox();
        outbox.add("+85511111111", "first");
        outbox.add("+85522222222", "second");

        List<DevSms> recent = outbox.recent();

        assertThat(recent).extracting(DevSms::message).containsExactly("second", "first");
        assertThat(recent.get(0).phone()).isEqualTo("+85522222222");
        assertThat(recent.get(0).sentAt()).isNotNull();
    }

    @Test
    void keepsOnlyTheNewestMessages() {
        DevSmsOutbox outbox = new DevSmsOutbox();
        int total = DevSmsOutbox.MAX_MESSAGES + 10;
        for (int i = 0; i < total; i++) {
            outbox.add("+85511111111", "msg-" + i);
        }

        List<DevSms> recent = outbox.recent();

        assertThat(recent).hasSize(DevSmsOutbox.MAX_MESSAGES);
        assertThat(recent.get(0).message()).isEqualTo("msg-" + (total - 1));
        assertThat(recent.get(recent.size() - 1).message()).isEqualTo("msg-10");
    }

    @Test
    void returnedListIsASnapshot() {
        DevSmsOutbox outbox = new DevSmsOutbox();
        outbox.add("+85511111111", "one");
        List<DevSms> snapshot = outbox.recent();

        outbox.add("+85511111111", "two");

        assertThat(snapshot).hasSize(1);
    }
}
