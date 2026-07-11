package com.checkout.payment.gateway.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.PaymentGatewayServiceImpl;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    @Mock
    private PaymentsRepository paymentsRepository;

    @Mock
    private BankSimulatorClient bankSimulatorClient;

    private PaymentGatewayServiceImpl paymentGatewayService;

    @BeforeEach
    void setUp() {
        paymentGatewayService = new PaymentGatewayServiceImpl(paymentsRepository, bankSimulatorClient);
    }

    @Test
    void whenBankAuthorizesPaymentThenStatusIsAuthorized() {
        YearMonth futureExpiry = YearMonth.now().plusMonths(1);
        PaymentRequest request = new PaymentRequest(
                "2222405343248871",
                futureExpiry.getMonthValue(),
                futureExpiry.getYear(),
                "GBP",
                100,
                "123");

        when(bankSimulatorClient.submitPayment(request))
                .thenReturn(new BankPaymentResponse(true, "auth-code"));

        PaymentResponse response = paymentGatewayService.processPayment(request, null);

        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.cardNumberLastFour()).isEqualTo("8871");
        assertThat(response.currency()).isEqualTo("GBP");
        assertThat(response.amount()).isEqualTo(100);

        ArgumentCaptor<PaymentResponse> captor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentsRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void whenBankDeclinesPaymentThenStatusIsDeclined() {
        YearMonth futureExpiry = YearMonth.now().plusMonths(1);
        PaymentRequest request = new PaymentRequest(
                "2222405343248872",
                futureExpiry.getMonthValue(),
                futureExpiry.getYear(),
                "USD",
                250,
                "456");

        when(bankSimulatorClient.submitPayment(any()))
                .thenReturn(new BankPaymentResponse(false, null));

        PaymentResponse response = paymentGatewayService.processPayment(request, null);

        assertThat(response.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.cardNumberLastFour()).isEqualTo("8872");
        verify(paymentsRepository).save(any(PaymentResponse.class));
    }

    @Test
    void whenPaymentDoesNotExistThenExceptionIsThrown() {
        UUID id = UUID.randomUUID();
        when(paymentsRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> paymentGatewayService.getPaymentById(id))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment not found");
    }
}
