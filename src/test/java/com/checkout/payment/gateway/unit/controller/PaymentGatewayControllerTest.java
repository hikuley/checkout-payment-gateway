package com.checkout.payment.gateway.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.Nested;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PaymentsRepository paymentsRepository;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private Map<String, Object> validPaymentRequest() {
        YearMonth futureExpiry = YearMonth.now().plusMonths(6);
        return Map.of(
                "cardNumber", "2222405343248871",
                "expiryMonth", futureExpiry.getMonthValue(),
                "expiryYear", futureExpiry.getYear(),
                "currency", "GBP",
                "amount", 500,
                "cvv", "456");
    }

    // ───────────────────────────────────────────────────────────────
    // POST /api/payments — Happy Paths
    // ───────────────────────────────────────────────────────────────

    @Nested
    class CreatePaymentHappyPaths {

        @Test
        void shouldReturnAuthorizedWhenBankApprovesPayment() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-001"));

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
                    .andExpect(jsonPath("$.cardNumberLastFour").value("8871"))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.amount").value(500))
                    .andExpect(jsonPath("$.expiryMonth").value(YearMonth.now().plusMonths(6).getMonthValue()))
                    .andExpect(jsonPath("$.expiryYear").value(YearMonth.now().plusMonths(6).getYear()));
        }

        @Test
        void shouldReturnDeclinedWhenBankDeclinesPayment() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(false, null));

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
        }

        @Test
        void shouldMaskCardNumberReturningOnlyLastFourDigits() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-002"));

            YearMonth futureExpiry = YearMonth.now().plusMonths(3);
            Map<String, Object> request = Map.of(
                    "cardNumber", "4111111111111234",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "USD",
                    "amount", 250,
                    "cvv", "789");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cardNumberLastFour").value("1234"))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        void shouldAcceptPaymentWithEURCurrency() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-eur"));

            YearMonth futureExpiry = YearMonth.now().plusMonths(2);
            Map<String, Object> request = Map.of(
                    "cardNumber", "4111111111115678",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "EUR",
                    "amount", 1000,
                    "cvv", "321");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.amount").value(1000));
        }

        @Test
        void shouldAcceptFourDigitCvv() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-cvv4"));

            YearMonth futureExpiry = YearMonth.now().plusMonths(4);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 75,
                    "cvv", "1234");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(75));
        }

        @Test
        void shouldAcceptMinimumLengthCardNumber() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-mincard"));

            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "22224053432488", // 14 digits — minimum length
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 1,
                    "cvv", "100");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cardNumberLastFour").value("2488"))
                    .andExpect(jsonPath("$.amount").value(1));
        }

        @Test
        void shouldAcceptMaximumLengthCardNumber() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-maxcard"));

            YearMonth futureExpiry = YearMonth.now().plusMonths(5);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248877777", // 19 digits — maximum length
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "USD",
                    "amount", 999,
                    "cvv", "999");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cardNumberLastFour").value("7777"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // POST /api/payments — Validation Errors (400)
    // ───────────────────────────────────────────────────────────────

    @Nested
    class CreatePaymentValidationErrors {

        @Test
        void shouldReject_whenCardNumberIsTooShort() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "1234567890123", // 13 digits — below 14
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.cardNumber").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCardNumberIsTooLong() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "12345678901234567890", // 20 digits — above 19
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cardNumber").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCardNumberContainsNonNumericCharacters() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222-4053-4324-8871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cardNumber").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenExpiryDateIsInThePast() throws Exception {
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", 1,
                    "expiryYear", 2020,
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenExpiryMonthIsGreaterThan12() throws Exception {
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", 13,
                    "expiryYear", 2030,
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.expiryMonth").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenExpiryMonthIsLessThan1() throws Exception {
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", 0,
                    "expiryYear", 2030,
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.expiryMonth").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCurrencyIsNotSupported() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "JPY",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.currency").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCurrencyLengthIsNot3() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBPP",
                    "amount", 100,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.currency").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenAmountIsZero() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 0,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.amount").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenAmountIsNegative() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", -10,
                    "cvv", "123");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.amount").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCvvIsTooShort() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "12");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cvv").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCvvIsTooLong() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "12345");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cvv").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenCvvContainsNonNumericCharacters() throws Exception {
            YearMonth futureExpiry = YearMonth.now().plusMonths(1);
            Map<String, Object> request = Map.of(
                    "cardNumber", "2222405343248871",
                    "expiryMonth", futureExpiry.getMonthValue(),
                    "expiryYear", futureExpiry.getYear(),
                    "currency", "GBP",
                    "amount", 100,
                    "cvv", "abc");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cvv").exists());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReject_whenRequestBodyIsEmpty() throws Exception {
            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verify(bankSimulatorClient, never()).submitPayment(any());
        }

        @Test
        void shouldReturnMultipleValidationErrors() throws Exception {
            Map<String, Object> request = Map.of(
                    "cardNumber", "abc",
                    "expiryMonth", 0,
                    "expiryYear", 2020,
                    "currency", "INVALID",
                    "amount", -1,
                    "cvv", "a");

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isNotEmpty());

            verify(bankSimulatorClient, never()).submitPayment(any());
        }
    }

    // ───────────────────────────────────────────────────────────────
    // POST /api/payments — Bank Unavailable (503)
    // ───────────────────────────────────────────────────────────────

    @Nested
    class CreatePaymentBankUnavailable {

        @Test
        void shouldReturn503WhenBankSimulatorIsUnavailable() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenThrow(new com.checkout.payment.gateway.exception.BankUnavailableException(
                            "Bank simulator is unavailable"));

            mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Bank simulator is unavailable"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // GET /api/payments/{id} — Happy Path
    // ───────────────────────────────────────────────────────────────

    @Nested
    class GetPaymentHappyPaths {

        @Test
        void shouldReturnPaymentWhenItExists() throws Exception {
            PaymentResponse payment = new PaymentResponse(
                    UUID.randomUUID(),
                    PaymentStatus.AUTHORIZED,
                    "4321",
                    6,
                    2028,
                    "USD",
                    750);
            paymentsRepository.save(payment);

            mockMvc().perform(get("/api/payments/" + payment.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(payment.id().toString()))
                    .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
                    .andExpect(jsonPath("$.cardNumberLastFour").value("4321"))
                    .andExpect(jsonPath("$.expiryMonth").value(6))
                    .andExpect(jsonPath("$.expiryYear").value(2028))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.amount").value(750));
        }

        @Test
        void shouldReturnDeclinedPaymentWhenItExists() throws Exception {
            PaymentResponse payment = new PaymentResponse(
                    UUID.randomUUID(),
                    PaymentStatus.DECLINED,
                    "9999",
                    3,
                    2029,
                    "EUR",
                    200);
            paymentsRepository.save(payment);

            mockMvc().perform(get("/api/payments/" + payment.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
                    .andExpect(jsonPath("$.cardNumberLastFour").value("9999"))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.amount").value(200));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // GET /api/payments/{id} — Not Found (404)
    // ───────────────────────────────────────────────────────────────

    @Nested
    class GetPaymentNotFound {

        @Test
        void shouldReturn404WhenPaymentDoesNotExist() throws Exception {
            mockMvc().perform(get("/api/payments/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Payment not found"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // End-to-End Flow: Create → then Retrieve
    // ───────────────────────────────────────────────────────────────

    @Nested
    class EndToEndFlow {

        @Test
        void shouldPersistAuthorizedPaymentAndRetrieveItById() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(true, "auth-e2e"));

            String responseBody = mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            PaymentResponse created = objectMapper.readValue(responseBody, PaymentResponse.class);

            mockMvc().perform(get("/api/payments/" + created.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(created.id().toString()))
                    .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
                    .andExpect(jsonPath("$.cardNumberLastFour").value("8871"))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.amount").value(500));
        }

        @Test
        void shouldPersistDeclinedPaymentAndRetrieveItById() throws Exception {
            when(bankSimulatorClient.submitPayment(any()))
                    .thenReturn(new BankPaymentResponse(false, null));

            String responseBody = mockMvc().perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPaymentRequest())))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            PaymentResponse created = objectMapper.readValue(responseBody, PaymentResponse.class);

            mockMvc().perform(get("/api/payments/" + created.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
        }
    }
}
