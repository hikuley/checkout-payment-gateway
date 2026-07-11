package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.UUID;

public interface PaymentGatewayService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(UUID id);
}
