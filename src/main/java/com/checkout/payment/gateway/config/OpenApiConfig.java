package com.checkout.payment.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the OpenAPI (Swagger) documentation.
 *
 * <p>Registers a customised {@link OpenAPI} bean that sets the API title, version,
 * and description shown in the Swagger UI at {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Produces the {@link OpenAPI} bean with project-specific metadata.
     *
     * @return a configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Checkout Payment Gateway API")
                        .version("1.0")
                        .description("API documentation for the payment gateway service"));
    }
}
