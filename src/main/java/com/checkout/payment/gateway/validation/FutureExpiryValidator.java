package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class FutureExpiryValidator implements ConstraintValidator<FutureExpiry, PaymentRequest> {

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
