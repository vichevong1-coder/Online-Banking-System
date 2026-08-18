package com.obs.backend.feature.transfer.dto;

import com.obs.backend.feature.account.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * US-026: a transfer from one of the caller's own accounts to an account at
 * another bank. Interbank settlement is simulated — see
 * {@code TransferServiceImpl#transferExternal}.
 *
 * <p>The destination is named by bank code + account number rather than by an
 * id: it is not a row we hold. The payee's <em>name</em> is deliberately not a
 * field here — a named payee is a beneficiary (US-029), and the transfer row has
 * nowhere to keep one.
 *
 * <p>As with {@link CreateTransferRequest}, Bean Validation covers only what is
 * decidable from the payload alone; ownership, currency, limits and available
 * funds need the source account loaded and are reported as coded 4xx errors.
 */
public record CreateExternalTransferRequest(
        @NotNull UUID fromAccountId,
        // SWIFT/BIC-shaped, uppercase alphanumeric. Not validated against a real
        // directory: there is no interbank integration to validate it against.
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{4,11}$") String beneficiaryBankCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{6,34}$") String beneficiaryAccountNumber,
        // Sent explicitly so the client states what it thinks it is moving; the
        // service rejects it if it disagrees with the source account.
        @NotNull Currency currency,
        // Four decimal places matches transfers.amount NUMERIC(19,4).
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 255) String description) {
}
