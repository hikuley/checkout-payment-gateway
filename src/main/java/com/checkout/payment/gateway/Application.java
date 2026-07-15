package com.checkout.payment.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Checkout Payment Gateway Spring Boot application.
 */
@SpringBootApplication
public class Application {

	/**
	 * Bootstraps and launches the application.
	 *
	 * @param args command-line arguments passed to the Spring context
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
