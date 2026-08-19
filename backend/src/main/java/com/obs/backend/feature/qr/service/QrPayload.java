package com.obs.backend.feature.qr.service;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.qr.exception.QrPayloadInvalidException;
import java.math.BigDecimal;

/**
 * The scanned string, parsed: {@code OBS1:<type>:<target>[:<currency>:<amount>]}.
 *
 * <p>A short delimited ASCII string rather than JSON or a URL, because the
 * Sprint 5 mobile half is meant to be a camera plus a POST and nothing else
 * (qr-payments-spec.md). There is deliberately no signature, expiry or nonce: a
 * payload names only a destination, and every authorisation that matters happens
 * server-side on {@code POST /qr/pay}.
 *
 * <p>{@code currency} and {@code amount} are both null or both set — a merchant
 * asking for an exact price, versus a code the payer types an amount into.
 */
public record QrPayload(QrTargetType type, String target, Currency currency, BigDecimal amount) {

    /** Format marker and version. A payload not starting with it is rejected unparsed. */
    private static final String MARKER = "OBS1";

    private static final String DELIMITER = ":";

    /** Matches transfers.amount NUMERIC(19,4). */
    private static final int MAX_AMOUNT_SCALE = 4;

    /** What a personal QR (US-032) carries: an account number, no amount. */
    public static String forAccountNumber(String accountNumber) {
        return String.join(DELIMITER, MARKER, QrTargetType.P.name(), accountNumber);
    }

    /**
     * Every rejection here is {@code 400 QR_PAYLOAD_INVALID}: an unparseable
     * string is a bad scan, and telling the payer which character was wrong helps
     * nobody holding a phone at a sticker.
     */
    public static QrPayload parse(String raw) {
        if (raw == null) {
            throw new QrPayloadInvalidException();
        }
        String[] parts = raw.trim().split(DELIMITER, -1);
        if (parts.length != 3 && parts.length != 5) {
            throw new QrPayloadInvalidException();
        }
        if (!MARKER.equals(parts[0])) {
            throw new QrPayloadInvalidException();
        }

        QrTargetType type = parseType(parts[1]);
        String target = parts[2];
        if (target.isBlank()) {
            throw new QrPayloadInvalidException();
        }
        if (parts.length == 3) {
            return new QrPayload(type, target, null, null);
        }
        return new QrPayload(type, target, parseCurrency(parts[3]), parseAmount(parts[4]));
    }

    /** True when the payload fixes the price, so the request may only agree with it. */
    public boolean hasAmount() {
        return amount != null;
    }

    private static QrTargetType parseType(String raw) {
        try {
            return QrTargetType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new QrPayloadInvalidException();
        }
    }

    private static Currency parseCurrency(String raw) {
        try {
            return Currency.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new QrPayloadInvalidException();
        }
    }

    private static BigDecimal parseAmount(String raw) {
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new QrPayloadInvalidException();
        }
        // Scale and sign are checked here rather than left to the column: an
        // amount the ledger cannot store is a malformed payload, not a failed
        // settlement.
        if (parsed.signum() <= 0 || parsed.scale() > MAX_AMOUNT_SCALE) {
            throw new QrPayloadInvalidException();
        }
        return parsed;
    }
}
