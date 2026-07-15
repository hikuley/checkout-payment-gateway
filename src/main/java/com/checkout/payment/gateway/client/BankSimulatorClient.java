package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for communicating with the external bank simulator.
 *
 * <p>Translates internal {@link PaymentRequest} objects into the bank's
 * expected format and maps error responses to domain exceptions.
 * The target URL is configured via the {@code bank.simulator.url} property.
 */
@Component
public class BankSimulatorClient {

    private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorClient.class);

    private final RestTemplate restTemplate;
    private final String bankSimulatorUrl;
    private final Retry retry;

    /**
     * @param restTemplate     pre-configured HTTP client
     * @param bankSimulatorUrl base URL of the bank simulator, injected from {@code bank.simulator.url}
     * @param retry            Resilience4j retry policy for transient bank failures
     */
    public BankSimulatorClient(RestTemplate restTemplate,
                               @Value("${bank.simulator.url}") String bankSimulatorUrl,
                               Retry retry) {
        this.restTemplate = restTemplate;
        this.bankSimulatorUrl = bankSimulatorUrl;
        this.retry = retry;
    }

    /**
     * Submits a payment to the bank simulator and returns the authorization result.
     *
     * @param request the payment request containing card and amount details
     * <p>Transient failures — the bank reporting itself unavailable (HTTP 503) or a
     * connection/read timeout — are retried according to the {@code bankSimulator}
     * retry policy before the exception is propagated.
     *
     * @return the bank's response indicating whether the payment was authorized
     * @throws BankUnavailableException if the bank simulator responds with HTTP 503
     *                                  on every attempt
     */
    public BankPaymentResponse submitPayment(PaymentRequest request) {
        return retry.executeSupplier(() -> submitPaymentOnce(request));
    }

    /**
     * Performs a single, un-retried payment submission to the bank simulator.
     *
     * @param request the payment request containing card and amount details
     * @return the bank's response indicating whether the payment was authorized
     * @throws BankUnavailableException if the bank simulator responds with HTTP 503
     */
    private BankPaymentResponse submitPaymentOnce(PaymentRequest request) {
        LOG.debug("Submitting payment to bank simulator at {}", bankSimulatorUrl);

        BankPaymentRequest bankRequest = toBankRequest(request);

        try {
            return restTemplate.postForObject(bankSimulatorUrl, bankRequest, BankPaymentResponse.class);
        } catch (HttpStatusCodeException ex) {
            handleBankException(ex);
            throw ex;
        }
    }

    /**
     * Maps HTTP error codes from the bank simulator to domain exceptions.
     *
     * @param ex the HTTP exception returned by the bank simulator
     * @throws BankUnavailableException if the status code is 503 Service Unavailable
     */
    private void handleBankException(HttpStatusCodeException ex) {
        if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new BankUnavailableException("Bank simulator is unavailable");
        }
    }

    /**
     * Converts an internal {@link PaymentRequest} to the bank's {@link BankPaymentRequest} format.
     *
     * <p>The expiry date is formatted as {@code MM/YYYY} as required by the bank API.
     *
     * @param request the internal payment request
     * @return a {@link BankPaymentRequest} ready to be sent to the bank simulator
     */
    private BankPaymentRequest toBankRequest(PaymentRequest request) {
        String expiryDate = String.format("%02d/%d", request.expiryMonth(), request.expiryYear());
        return new BankPaymentRequest(
                request.cardNumber(),
                expiryDate,
                request.currency(),
                request.amount(),
                request.cvv()
        );
    }
}
