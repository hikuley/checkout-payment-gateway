package com.checkout.payment.gateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that a given currency code is present in the configured set of allowed currencies.
 *
 * <p>The allowed currencies are injected from the {@code payment.allowed-currencies}
 * application property as a comma-separated list (e.g. {@code USD,GBP,EUR}).
 * Comparison is case-insensitive. {@code null} values are considered valid and
 * deferred to {@code @NotNull} or {@code @NotBlank} constraints.
 */
@Component
public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

    private Set<String> allowedCurrencies;

    /**
     * Populates the set of allowed currencies from the application configuration.
     *
     * @param currencies comma-separated list of supported ISO 4217 currency codes
     */
    @Value("${payment.allowed-currencies}")
    public void setAllowedCurrencies(String currencies) {
        this.allowedCurrencies = Arrays.stream(currencies.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    /**
     * Returns {@code true} if {@code value} is {@code null} or matches one of the
     * allowed currency codes (case-insensitive).
     *
     * @param value   the currency code to validate
     * @param context constraint validator context (unused)
     * @return {@code true} if the value is acceptable, {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedCurrencies.contains(value.toUpperCase());
    }
}
