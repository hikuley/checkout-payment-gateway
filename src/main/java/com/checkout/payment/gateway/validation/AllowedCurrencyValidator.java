package com.checkout.payment.gateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

    private Set<String> allowedCurrencies;

    @Value("${payment.allowed-currencies}")
    public void setAllowedCurrencies(String currencies) {
        this.allowedCurrencies = Arrays.stream(currencies.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedCurrencies.contains(value.toUpperCase());
    }
}
