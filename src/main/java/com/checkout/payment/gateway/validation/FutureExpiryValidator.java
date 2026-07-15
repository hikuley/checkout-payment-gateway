package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

/**
 * Validates that the expiry date on a {@link PaymentRequest} is strictly in the future.
 *
 * <p>Constructs a {@link YearMonth} from the request's {@code expiryYear} and
 * {@code expiryMonth} fields and compares it against the current month. Returns
 * {@code true} for {@code null} requests or missing fields, deferring to individual
 * field-level {@code @NotNull} constraints.
 */
public class FutureExpiryValidator implements ConstraintValidator<FutureExpiry, PaymentRequest> {

    /**
     * Returns {@code true} if the card expiry is after the current year-month.
     *
     * @param request the payment request to validate
     * @param context constraint validator context (unused)
     * @return {@code true} if valid or fields are absent; {@code false} if already expired
     */
    @Override
    public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.expiryMonth() == null || request.expiryYear() == null) {
            return true;
        }

        try {
            YearMonth expiry = YearMonth.of(request.expiryYear(), request.expiryMonth());
            return expiry.isAfter(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }
}
