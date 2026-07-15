package com.checkout.payment.gateway.model;

import java.util.Map;

/**
 * Standardised error payload returned by the API on failure.
 *
 * @param message top-level human-readable description of the error
 * @param errors  optional map of field-level validation errors, keyed by field name;
 *                {@code null} when there are no field-specific details
 */
public record ErrorResponse(String message, Map<String, String> errors) {

    /**
     * Convenience constructor for errors without field-level detail.
     *
     * @param message top-level error description
     */
    public ErrorResponse(String message) {
        this(message, null);
    }
}
