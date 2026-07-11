package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankSimulatorClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PaymentGatewayControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void whenPaymentIsValidThenAuthorizedPaymentIsReturned() throws Exception {
        when(bankSimulatorClient.submitPayment(any())).thenReturn(
                new BankPaymentResponse(true, "auth-code-123"));

        YearMonth futureExpiry = YearMonth.now().plusMonths(1);
        Map<String, Object> request = Map.of(
                "cardNumber", "2222405343248871",
                "expiryMonth", futureExpiry.getMonthValue(),
                "expiryYear", futureExpiry.getYear(),
                "currency", "GBP",
                "amount", 100,
                "cvv", "123"
        );

        mockMvc().perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
                .andExpect(jsonPath("$.cardNumberLastFour").value("8871"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void whenPaymentIsInvalidThen400IsReturned() throws Exception {
        Map<String, Object> request = Map.of(
                "cardNumber", "123",
                "expiryMonth", 1,
                "expiryYear", 2020,
                "currency", "INVALID",
                "amount", 0,
                "cvv", "12"
        );

        mockMvc().perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void whenPaymentWithIdExistsThenCorrectPaymentIsReturned() throws Exception {
        PaymentResponse payment = new PaymentResponse(
                UUID.randomUUID(),
                PaymentStatus.AUTHORIZED,
                "8871",
                12,
                2030,
                "GBP",
                100
        );
        paymentsRepository.save(payment);

        mockMvc().perform(get("/api/payments/" + payment.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.id().toString()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
                .andExpect(jsonPath("$.cardNumberLastFour").value("8871"))
                .andExpect(jsonPath("$.expiryMonth").value(12))
                .andExpect(jsonPath("$.expiryYear").value(2030))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
        mockMvc().perform(get("/api/payments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found"));
    }
}
