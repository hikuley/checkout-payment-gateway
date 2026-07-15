package com.checkout.payment.gateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint that ensures a {@link com.checkout.payment.gateway.model.PaymentRequest}'s
 * card expiry date is in the future.
 *
 * <p>Applied at the type level so it can access both {@code expiryMonth} and
 * {@code expiryYear} together. Enforced by {@link FutureExpiryValidator}.
 *
 * <p>Usage:
 * <pre>{@code
 * @FutureExpiry
 * public record PaymentRequest(...) {}
 * }</pre>
 */
@Documented
@Constraint(validatedBy = FutureExpiryValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureExpiry {

    String message() default "Expiry date must be in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
