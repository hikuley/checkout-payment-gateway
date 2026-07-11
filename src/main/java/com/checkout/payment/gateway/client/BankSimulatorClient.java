package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankSimulatorClient {

    private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorClient.class);

    private final RestTemplate restTemplate;
    private final String bankSimulatorUrl;

    public BankSimulatorClient(RestTemplate restTemplate, @Value("${bank.simulator.url}") String bankSimulatorUrl) {
        this.restTemplate = restTemplate;
        this.bankSimulatorUrl = bankSimulatorUrl;
    }

    public BankPaymentResponse submitPayment(PaymentRequest request) {
        LOG.debug("Submitting payment to bank simulator at {}", bankSimulatorUrl);

        BankPaymentRequest bankRequest = toBankRequest(request);

        try {
            return restTemplate.postForObject(bankSimulatorUrl, bankRequest, BankPaymentResponse.class);
        } catch (HttpStatusCodeException ex) {
            handleBankException(ex);
            throw ex;
        }
    }

    private void handleBankException(HttpStatusCodeException ex) {
        if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new BankUnavailableException("Bank simulator is unavailable");
        }
    }

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
