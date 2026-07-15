package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Payment Gateway API.
 *
 * <p>Handles HTTP requests for creating and retrieving payment transactions.
 * All endpoints are mounted under {@code /api/payments}.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    /**
     * @param paymentGatewayService service used to process and retrieve payments
     */
    public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Initiates a new payment transaction.
     *
     * <p>An optional {@code X-Idempotency-Key} header can be supplied so that
     * retried requests with the same key return the original response without
     * reprocessing the payment.
     *
     * @param idempotencyKey optional client-supplied key for idempotent retries
     * @param request        validated payment details
     * @return {@code 201 Created} with the resulting {@link PaymentResponse}
     */
    @Operation(summary = "Create a payment", description = "Initiates a new payment transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "503", description = "Bank simulator unavailable")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PaymentRequest request) {
        PaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the details of a previously processed payment.
     *
     * @param id unique identifier of the payment
     * @return {@code 200 OK} with the {@link PaymentResponse}, or {@code 404} if not found
     */
    @Operation(summary = "Get a payment", description = "Retrieves details of a previously made payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        PaymentResponse response = paymentGatewayService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }
}
