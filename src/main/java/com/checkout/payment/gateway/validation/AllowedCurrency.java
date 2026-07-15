package com.checkout.payment.gateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a currency code is in the list of supported currencies.
 *
 * <p>The allowed values are configured via the {@code payment.allowed-currencies}
 * property and enforced by {@link AllowedCurrencyValidator}.
 *
 * <p>Usage:
 * <pre>{@code
 * @AllowedCurrency
 * String currency;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = AllowedCurrencyValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedCurrency {

    String message() default "Currency is not supported";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
