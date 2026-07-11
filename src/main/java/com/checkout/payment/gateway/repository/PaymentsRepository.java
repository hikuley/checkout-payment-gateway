package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentsRepository.class);

    private final Map<UUID, PaymentResponse> payments = new ConcurrentHashMap<>();

    public void save(PaymentResponse payment) {
        LOG.debug("Saving payment with ID {}", payment.id());
        payments.put(payment.id(), payment);
    }

    public Optional<PaymentResponse> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }
}
