package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response received from the bank simulator after submitting a payment.
 *
 * @param authorized        {@code true} if the bank approved the payment, {@code false} otherwise
 * @param authorizationCode bank-issued code identifying the approved transaction;
 *                          {@code null} when the payment was not authorized
 */
public record BankPaymentResponse(
        boolean authorized,
        @JsonProperty("authorization_code") String authorizationCode
) {
}
