package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayServiceImpl.class);

    private final PaymentsRepository paymentsRepository;
    private final BankSimulatorClient bankSimulatorClient;
    private final java.util.Map<String, PaymentResponse> idempotencyCache = new java.util.concurrent.ConcurrentHashMap<>();

    public PaymentGatewayServiceImpl(PaymentsRepository paymentsRepository, BankSimulatorClient bankSimulatorClient) {
        this.paymentsRepository = paymentsRepository;
        this.bankSimulatorClient = bankSimulatorClient;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        // Check cache first for idempotent requests to prevent duplicate processing
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            LOG.debug("Returning cached response for idempotency key: {}", idempotencyKey);
            return idempotencyCache.get(idempotencyKey);
        }

        // Generate unique payment ID and extract masked card number for security
        UUID id = UUID.randomUUID();
        String cardNumberLastFour = extractLastFourDigits(request.cardNumber());

        // Submit payment to bank simulator and determine transaction status
        BankPaymentResponse bankResponse = bankSimulatorClient.submitPayment(request);
        PaymentStatus status = bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

        // Build payment response with transaction details (card info masked)
        PaymentResponse payment = new PaymentResponse(id, status, cardNumberLastFour, request.expiryMonth(), request.expiryYear(), request.currency(), request.amount());

        // Persist payment record to database for audit trail
        paymentsRepository.save(payment);

        // Cache the response for idempotency if the key is provided to handle retries
        if (idempotencyKey != null) {
            idempotencyCache.put(idempotencyKey, payment);
        }

        LOG.debug("Processed payment {} with status {}", id, status);
        return payment;
    }

    @Override
    public PaymentResponse getPaymentById(UUID id) {
        LOG.debug("Retrieving payment with ID {}", id);
        return paymentsRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
    }

    private String extractLastFourDigits(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
