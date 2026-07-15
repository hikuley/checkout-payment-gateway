package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link PaymentGatewayService}.
 *
 * <p>Orchestrates the full payment lifecycle:
 * <ol>
 *   <li>Checks an in-memory idempotency cache to short-circuit duplicate requests.</li>
 *   <li>Forwards the payment to the {@link BankSimulatorClient}.</li>
 *   <li>Persists the result via {@link PaymentsRepository}.</li>
 *   <li>Populates the idempotency cache for future retries.</li>
 * </ol>
 */
@Service
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayServiceImpl.class);

    private final PaymentsRepository paymentsRepository;
    private final BankSimulatorClient bankSimulatorClient;

    /**
     * In-memory store mapping idempotency keys to their cached responses.
     * Uses a {@link ConcurrentHashMap} to support concurrent request handling.
     */
    private final Map<String, PaymentResponse> idempotencyCache = new ConcurrentHashMap<>();

    /**
     * @param paymentsRepository  repository for persisting and retrieving payment records
     * @param bankSimulatorClient client used to submit payments to the bank simulator
     */
    public PaymentGatewayServiceImpl(PaymentsRepository paymentsRepository, BankSimulatorClient bankSimulatorClient) {
        this.paymentsRepository = paymentsRepository;
        this.bankSimulatorClient = bankSimulatorClient;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Card details are masked in the stored response — only the last four
     * digits of the card number are retained.
     *
     * @throws com.checkout.payment.gateway.exception.BankUnavailableException if the bank simulator returns a 503
     */
    @Override
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        // Check the cache first for idempotent requests to prevent duplicate processing
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            LOG.debug("Returning cached response for idempotency key: {}", idempotencyKey);
            return idempotencyCache.get(idempotencyKey);
        } else if (idempotencyKey != null) {
            LOG.debug("No cached response found for idempotency key: {}", idempotencyKey);
        }

        // Generate unique payment ID and extract masked card number for security
        final UUID id = UUID.randomUUID();
        final String cardNumberLastFour = extractLastFourDigits(request.cardNumber());

        // Submit payment to a bank simulator and determine transaction status
        final BankPaymentResponse bankResponse = bankSimulatorClient.submitPayment(request);
        final PaymentStatus status = bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

        // Build a payment response with transaction details (card info masked)
        final PaymentResponse payment = new PaymentResponse(
                id,
                status,
                cardNumberLastFour,
                request.expiryMonth(),
                request.expiryYear(),
                request.currency(),
                request.amount()
        );

        // Persist payment record to a database for audit trail
        paymentsRepository.save(payment);

        // Cache the response for idempotency if the key is provided to handle retries
        if (idempotencyKey != null) {
            idempotencyCache.put(idempotencyKey, payment);
        }

        LOG.debug("Processed payment {} with status {}", id, status);
        return payment;
    }

    /** {@inheritDoc} */
    @Override
    public PaymentResponse getPaymentById(UUID id) {
        LOG.debug("Retrieving payment with ID {}", id);
        return paymentsRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    /**
     * Extracts the last four digits of a card number for masked display.
     *
     * @param cardNumber the full card number
     * @return the last four characters of {@code cardNumber}
     */
    private String extractLastFourDigits(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
