package com.obs.backend.feature.notification.listener;

import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.service.NotificationService;
import com.obs.backend.feature.transfer.event.TransferCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes the US-035 notification after the transfer that caused it has committed.
 *
 * <p>It used to be written inline, inside the caller's transaction. Because
 * {@code sendNotification} is {@code @Transactional(REQUIRED)} it joined that transaction,
 * so a failure there marked it rollback-only: TransferSupport's catch could log the problem
 * but the commit still failed, losing a settled transfer because the customer could not be
 * told about it. Bill payments (US-039) made that a third caller and forced the fix.
 *
 * <p>AFTER_COMMIT rather than REQUIRES_NEW. REQUIRES_NEW suspends the caller's transaction
 * and so cannot see rows it has not committed yet — for a brand-new customer the
 * notification's user_id FK fails outright — and it would also commit a "Transfer completed"
 * notification for a transfer whose transaction still went on to fail. Waiting for the
 * commit avoids both.
 *
 * <p>REQUIRES_NEW here is not the same trade: by AFTER_COMMIT the caller's transaction is
 * finished, so this needs a transaction of its own simply because there is none left to join.
 */
@Component
public class TransferCompletedNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(TransferCompletedNotificationListener.class);

    private final NotificationService notificationService;

    public TransferCompletedNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferCompleted(TransferCompletedEvent event) {
        try {
            notificationService.sendNotification(
                    event.userId(), "Transfer completed", event.message(), NotificationType.TRANSFER);
        } catch (RuntimeException e) {
            // The money has already committed. Failing here must not surface to the caller —
            // it cannot un-move the funds, and the transfer itself is sound.
            log.warn("Transfer completed but its notification could not be sent for user {}", event.userId(), e);
        }
    }
}
