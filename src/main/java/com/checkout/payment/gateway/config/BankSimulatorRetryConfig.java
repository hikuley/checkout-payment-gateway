package com.checkout.payment.gateway.config;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;

/**
 * Configures the Resilience4j {@link Retry} used by the bank simulator client.
 *
 * <p>Retries are attempted only for transient failures — a bank reporting itself
 * unavailable ({@link BankUnavailableException}, mapped from HTTP 503) and low-level
 * connection/read timeouts ({@link ResourceAccessException}). Business outcomes such
 * as a declined payment or a 4xx response are never retried.
 *
 * <p>Waits between attempts grow exponentially from a configurable initial interval.
 */
@Configuration
public class BankSimulatorRetryConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorRetryConfig.class);

    private final int maxAttempts;
    private final long initialIntervalMs;
    private final double backoffMultiplier;

    /**
     * @param maxAttempts       total number of attempts, including the first call
     * @param initialIntervalMs wait before the first retry, in milliseconds
     * @param backoffMultiplier factor by which the wait grows after each attempt
     */
    public BankSimulatorRetryConfig(
            @Value("${bank.simulator.retry.max-attempts:3}") int maxAttempts,
            @Value("${bank.simulator.retry.initial-interval-ms:500}") long initialIntervalMs,
            @Value("${bank.simulator.retry.backoff-multiplier:2}") double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialIntervalMs = initialIntervalMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    /**
     * Builds the {@link Retry} instance used when submitting payments to the bank.
     *
     * @return a configured {@link Retry} named {@code bankSimulator}
     */
    @Bean
    public Retry bankSimulatorRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialIntervalMs, backoffMultiplier))
                .retryExceptions(BankUnavailableException.class, ResourceAccessException.class)
                .build();

        Retry retry = Retry.of("bankSimulator", config);
        retry.getEventPublisher().onRetry(event ->
                LOG.warn("Retrying bank simulator call (attempt {}) after failure: {}",
                        event.getNumberOfRetryAttempts() + 1,
                        event.getLastThrowable() == null ? "unknown" : event.getLastThrowable().getMessage()));
        return retry;
    }
}
