package com.checkout.payment.gateway.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the outcome of a payment transaction.
 *
 * <ul>
 *   <li>{@link #AUTHORIZED} — the bank approved the payment.</li>
 *   <li>{@link #DECLINED} — the bank rejected the payment (e.g. insufficient funds).</li>
 *   <li>{@link #REJECTED} — the request was rejected before reaching the bank (e.g. validation failure).</li>
 * </ul>
 *
 * <p>The {@link JsonValue} annotation ensures the human-readable {@code name} is used
 * when serialising to JSON instead of the enum constant name.
 */
public enum PaymentStatus {
    /** The bank approved the payment. */
    AUTHORIZED("Authorized"),
    /** The bank declined the payment. */
    DECLINED("Declined"),
    /** The payment was rejected prior to bank submission. */
    REJECTED("Rejected");

    private final String name;

    PaymentStatus(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable name used for JSON serialisation.
     *
     * @return display name of this status
     */
    @JsonValue
    public String getName() {
        return this.name;
    }
}
