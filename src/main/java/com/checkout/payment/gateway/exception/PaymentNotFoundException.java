package com.checkout.payment.gateway.exception;

/**
 * Thrown when a requested payment cannot be found in the repository.
 *
 * <p>Handled by {@link GlobalExceptionHandler}, which maps it to an HTTP 404 response.
 */
public class PaymentNotFoundException extends RuntimeException {

    /**
     * @param message description of which payment could not be found
     */
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
