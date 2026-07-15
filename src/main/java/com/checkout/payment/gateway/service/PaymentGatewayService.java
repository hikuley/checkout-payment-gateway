package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.UUID;

/**
 * Core service interface for the Payment Gateway.
 *
 * <p>Defines the operations available to process new payments and retrieve
 * existing ones. Implementations are responsible for bank communication,
 * persistence, and idempotency handling.
 */
public interface PaymentGatewayService {

    /**
     * Processes a payment request by forwarding it to the bank simulator and
     * persisting the result.
     *
     * <p>When {@code idempotencyKey} is provided, a cached response is returned
     * for duplicate requests with the same key, preventing double-charges.
     *
     * @param request        the payment details to process
     * @param idempotencyKey optional key used to deduplicate retried requests
     * @return the resulting {@link PaymentResponse} containing the payment ID and status
     */
    PaymentResponse processPayment(PaymentRequest request, String idempotencyKey);

    /**
     * Retrieves a previously processed payment by its unique identifier.
     *
     * @param id the UUID of the payment
     * @return the {@link PaymentResponse} for the given ID
     * @throws com.checkout.payment.gateway.exception.PaymentNotFoundException if no payment exists with that ID
     */
    PaymentResponse getPaymentById(UUID id);
}
