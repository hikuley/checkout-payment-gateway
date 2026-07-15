package com.checkout.payment.gateway.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the HTTP client used to communicate with external services.
 *
 * <p>Provides a {@link RestTemplate} bean with explicit connection and read timeouts
 * to prevent threads from hanging on slow or unresponsive upstream services.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a {@link RestTemplate} bean with a 10-second connection timeout and a
     * 10-second read timeout.
     *
     * @return a configured {@link RestTemplate} instance
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(10000));
        factory.setReadTimeout(Duration.ofMillis(10000));
        return new RestTemplate(factory);
    }
}
