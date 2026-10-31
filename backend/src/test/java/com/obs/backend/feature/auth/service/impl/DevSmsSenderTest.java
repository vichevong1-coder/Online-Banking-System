package com.obs.backend.feature.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.obs.backend.feature.devtools.DevSmsOutbox;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class DevSmsSenderTest {

    private static final String PHONE = "+855 11-222-333";

    private final DevSmsOutbox outbox = new DevSmsOutbox();
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final DevSmsSender sender = new DevSmsSender(outbox, mailSender);

    @Test
    void recordsMessageInOutboxAndMirrorsToMailpit() {
        sender.send(PHONE, "482913");

        assertThat(outbox.recent()).hasSize(1);
        assertThat(outbox.recent().get(0).phone()).isEqualTo(PHONE);
        assertThat(outbox.recent().get(0).message()).contains("482913");

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("+85511222333@sms.obs.local");
        assertThat(mail.getValue().getSubject()).isEqualTo("SMS to " + PHONE);
        assertThat(mail.getValue().getText()).contains("482913");
    }

    @Test
    void doesNotThrowWhenMailpitIsDown() {
        doThrow(new MailSendException("connection refused"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatCode(() -> sender.send(PHONE, "482913")).doesNotThrowAnyException();

        assertThat(outbox.recent()).hasSize(1);
    }
}
