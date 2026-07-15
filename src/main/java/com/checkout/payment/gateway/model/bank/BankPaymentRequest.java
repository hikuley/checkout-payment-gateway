package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment request payload sent to the bank simulator.
 *
 * <p>Field names are mapped to snake_case via {@link JsonProperty} to match the
 * bank simulator's expected JSON contract.
 *
 * @param cardNumber  full card number
 * @param expiryDate  card expiry date formatted as {@code MM/YYYY}
 * @param currency    ISO 4217 currency code
 * @param amount      payment amount in the minor unit of the currency
 * @param cvv         card verification value (3 or 4 digits)
 */
public record BankPaymentRequest(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("expiry_date") String expiryDate,
        String currency,
        int amount,
        String cvv
) {
}
