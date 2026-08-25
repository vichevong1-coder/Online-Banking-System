package com.obs.backend.feature.transfer.event;

import java.util.UUID;

/**
 * Published by {@link com.obs.backend.feature.transfer.service.TransferSupport} once a
 * transfer has settled, and handled only after the publishing transaction commits.
 *
 * <p>It exists so the US-035 notification is not written inside the money-moving
 * transaction. See {@code TransferCompletedNotificationListener} for why that mattered.
 */
public record TransferCompletedEvent(UUID userId, String message) {
}
