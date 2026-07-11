package com.checkout.payment.gateway.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * Creates a RestTemplate bean with configured timeout settings for HTTP requests.
     * This bean is used for making RESTful API calls to external payment services
     * with predefined connection and read timeouts to prevent hanging requests.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Set connection timeout to 10 seconds to limit time spent establishing a connection
        factory.setConnectTimeout(Duration.ofMillis(10000));
        // Set read timeout to 10 seconds to limit time spent waiting for a response
        factory.setReadTimeout(Duration.ofMillis(10000));
        return new RestTemplate(factory);
    }
}
