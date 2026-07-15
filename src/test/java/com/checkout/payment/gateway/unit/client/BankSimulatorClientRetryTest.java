package com.checkout.payment.gateway.unit.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies that {@link BankSimulatorClient} applies the Resilience4j retry policy
 * correctly: transient failures are retried, permanent failures are not, and the
 * bank is called exactly the expected number of times in each case.
 *
 * <p>The retry used here mirrors the production policy (retrying
 * {@link BankUnavailableException} and {@link ResourceAccessException}) but with a
 * negligible wait so the test runs instantly.
 */
@DisplayName("BankSimulatorClient retry behaviour")
class BankSimulatorClientRetryTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final String BANK_URL = "http://bank.test/payments";

    @Mock
    private RestTemplate restTemplate;

    private BankSimulatorClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(BankUnavailableException.class, ResourceAccessException.class)
                .build();
        Retry retry = Retry.of("bankSimulator-test", config);
        client = new BankSimulatorClient(restTemplate, BANK_URL, retry);
    }

    private PaymentRequest validRequest() {
        YearMonth expiry = YearMonth.now().plusMonths(6);
        return new PaymentRequest(
                "2222405343248871", expiry.getMonthValue(), expiry.getYear(), "GBP", 500, "456");
    }

    @Test
    @DisplayName("retries after a transient 503 and succeeds")
    void shouldRetryOn503ThenSucceed() {
        BankPaymentResponse success = new BankPaymentResponse(true, "auth-1");
        when(restTemplate.postForObject(anyString(), any(), eq(BankPaymentResponse.class)))
                .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(success);

        BankPaymentResponse result = client.submitPayment(validRequest());

        assertTrue(result.authorized());
        assertEquals("auth-1", result.authorizationCode());
        verify(restTemplate, times(MAX_ATTEMPTS))
                .postForObject(anyString(), any(), eq(BankPaymentResponse.class));
    }

    @Test
    @DisplayName("retries after a transient connection failure and succeeds")
    void shouldRetryOnResourceAccessExceptionThenSucceed() {
        BankPaymentResponse success = new BankPaymentResponse(true, "auth-2");
        when(restTemplate.postForObject(anyString(), any(), eq(BankPaymentResponse.class)))
                .thenThrow(new ResourceAccessException("connection reset"))
                .thenReturn(success);

        BankPaymentResponse result = client.submitPayment(validRequest());

        assertTrue(result.authorized());
        verify(restTemplate, times(2))
                .postForObject(anyString(), any(), eq(BankPaymentResponse.class));
    }

    @Test
    @DisplayName("exhausts all attempts on persistent 503 then throws BankUnavailableException")
    void shouldExhaustRetriesAndThrow() {
        when(restTemplate.postForObject(anyString(), any(), eq(BankPaymentResponse.class)))
                .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(BankUnavailableException.class, () -> client.submitPayment(validRequest()));

        verify(restTemplate, times(MAX_ATTEMPTS))
                .postForObject(anyString(), any(), eq(BankPaymentResponse.class));
    }

    @Test
    @DisplayName("does not retry a non-transient 4xx error")
    void shouldNotRetryClientError() {
        when(restTemplate.postForObject(anyString(), any(), eq(BankPaymentResponse.class)))
                .thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThrows(HttpClientErrorException.class, () -> client.submitPayment(validRequest()));

        verify(restTemplate, times(1))
                .postForObject(anyString(), any(), eq(BankPaymentResponse.class));
    }
}
