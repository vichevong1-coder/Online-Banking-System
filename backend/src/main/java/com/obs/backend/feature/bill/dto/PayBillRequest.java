package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.account.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PayBillRequest(
        @NotNull(message = "Source account is required")
        UUID fromAccountId,

        @NotNull(message = "Provider ID is required")
        UUID providerId,

        @NotBlank(message = "Bill account number is required")
        String billAccountNumber,

        @NotNull(message = "Amount is required")
        // Four decimal places matches transfers.amount NUMERIC(19,4), the same bound
        // CreateTransferRequest/QrPayRequest carry. Without it a fractional amount
        // reaches setScale(4, UNNECESSARY) in the service and throws ArithmeticException
        // out of the controller as a 500 instead of a field-level 400.
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Amount cannot have more than 4 decimal places")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        Currency currency
) {}
