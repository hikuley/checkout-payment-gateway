package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PaymentResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * In-memory repository for storing and retrieving {@link PaymentResponse} records.
 *
 * <p>Backed by a {@link ConcurrentHashMap} keyed on payment UUID, making it safe
 * for concurrent access. Note that all data is lost on application restart — this
 * implementation is intended for development and testing purposes.
 */
@Repository
public class PaymentsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentsRepository.class);

    private final Map<UUID, PaymentResponse> payments = new ConcurrentHashMap<>();

    /**
     * Persists a payment record, overwriting any existing entry with the same ID.
     *
     * @param payment the payment response to store
     */
    public void save(PaymentResponse payment) {
        LOG.debug("Saving payment with ID {}", payment.id());
        payments.put(payment.id(), payment);
    }

    /**
     * Looks up a payment by its unique identifier.
     *
     * @param id the UUID of the payment
     * @return an {@link Optional} containing the payment if found, or empty if not
     */
    public Optional<PaymentResponse> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }

    /**
     * Removes all stored payments. Primarily used for resetting state between tests.
     */
    public void clear() {
        LOG.debug("Clearing all payments from repository");
        payments.clear();
    }
}
