package com.checkout.payment.gateway.exception;

/**
 * Thrown when the bank simulator responds with HTTP 503 Service Unavailable.
 *
 * <p>Handled by {@link GlobalExceptionHandler}, which maps it to an HTTP 503 response.
 */
public class BankUnavailableException extends RuntimeException {

    /**
     * @param message description of the bank availability failure
     */
    public BankUnavailableException(String message) {
        super(message);
    }
}
