package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.AllowedCurrency;
import com.checkout.payment.gateway.validation.FutureExpiry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@FutureExpiry
public record PaymentRequest(
        @NotBlank(message = "Card number is required")
        @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 characters")
        @Pattern(regexp = "\\d+", message = "Card number must only contain numeric characters")
        String cardNumber,

        @NotNull(message = "Expiry month is required")
        @Min(value = 1, message = "Expiry month must be between 1 and 12")
        @Max(value = 12, message = "Expiry month must be between 1 and 12")
        Integer expiryMonth,

        @NotNull(message = "Expiry year is required")
        Integer expiryYear,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        @AllowedCurrency
        String currency,

        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be a positive integer")
        Integer amount,

        @NotBlank(message = "CVV is required")
        @Size(min = 3, max = 4, message = "CVV must be 3 or 4 characters")
        @Pattern(regexp = "\\d+", message = "CVV must only contain numeric characters")
        String cvv
) {
}
