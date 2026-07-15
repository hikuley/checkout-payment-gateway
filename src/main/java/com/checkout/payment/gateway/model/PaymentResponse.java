package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;

import java.util.UUID;

/**
 * Represents the result of a processed payment transaction.
 *
 * <p>Sensitive card data is masked — only the last four digits of the card
 * number are retained. This record is both persisted in the repository and
 * returned to the caller.
 *
 * @param id                unique identifier assigned to this payment
 * @param status            outcome of the transaction (e.g. AUTHORIZED, DECLINED)
 * @param cardNumberLastFour last four digits of the card number used for the payment
 * @param expiryMonth       expiry month of the card (1–12)
 * @param expiryYear        expiry year of the card
 * @param currency          ISO 4217 currency code (e.g. USD, GBP)
 * @param amount            payment amount in the minor unit of the currency (e.g. pence, cents)
 */
public record PaymentResponse(
        UUID id,
        PaymentStatus status,
        String cardNumberLastFour,
        int expiryMonth,
        int expiryYear,
        String currency,
        int amount
) {

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "id=" + id +
                ", status=" + status +
                ", cardNumberLastFour='" + cardNumberLastFour + '\'' +
                ", expiryMonth=" + expiryMonth +
                ", expiryYear=" + expiryYear +
                ", currency='" + currency + '\'' +
                ", amount=" + amount +
                '}';
    }
}
