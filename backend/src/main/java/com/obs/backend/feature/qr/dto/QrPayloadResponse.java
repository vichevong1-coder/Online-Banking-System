package com.obs.backend.feature.qr.dto;

import com.obs.backend.feature.account.entity.Currency;
import java.util.UUID;

/**
 * US-032: the payload one of the caller's accounts is represented by.
 *
 * <p>A string, not an image. Rendering it as a QR bitmap is the Sprint 5 mobile
 * app's job — a Dart widget is a few lines, whereas returning a PNG would put
 * image encoding, sizing and caching in the API for no gain.
 */
public record QrPayloadResponse(UUID accountId, String accountNumber, Currency currency, String payload) {
}
