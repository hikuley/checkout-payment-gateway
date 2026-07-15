package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception handler for all controllers in the application.
 *
 * <p>Intercepts domain and validation exceptions and translates them into
 * consistent {@link ErrorResponse} payloads with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures triggered by {@code @Valid} on controller method arguments.
     *
     * <p>Collects all field-level constraint violations and any object-level constraint
     * messages (stored under the {@code _global} key) into a single error map.
     *
     * @param ex the validation exception containing binding result details
     * @return {@code 400 Bad Request} with a map of field-to-message validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String objectError = ex.getBindingResult().getGlobalErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(null);
        if (objectError != null) {
            errors.put("_global", objectError);
        }

        return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", errors));
    }

    /**
     * Handles the case where a requested payment does not exist.
     *
     * @param ex the exception carrying the not-found message
     * @return {@code 404 Not Found} with the error message
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        LOG.debug("Payment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles failures when the bank simulator is unreachable or unavailable.
     *
     * @param ex the exception carrying the unavailability message
     * @return {@code 503 Service Unavailable} with the error message
     */
    @ExceptionHandler(BankUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBankUnavailable(BankUnavailableException ex) {
        LOG.error("Bank unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(ex.getMessage()));
    }
}
