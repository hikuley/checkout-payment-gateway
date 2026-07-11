# Checkout.com Payment Gateway

This repository contains the implementation of the Checkout.com Engineering Assessment: Building a Payment Gateway API.

## Overview

The Payment Gateway is a robust, API-based application designed to allow merchants to easily process online card payments and query their status later. The gateway stands between the merchant and the acquiring bank, handling validations, interactions with the simulated bank, error management, and secure response handling.

## Key Features

- **Process Payments**: Submits payment details to a simulated acquiring bank subject to strict payload validations, enforcing correct structures for card numbers, cvv, expiry date, currency code, and positive amounts.
- **Retrieve Payments**: Allows merchants to fetch previously processed payment details safely, with card numbers securely masked.
- **Bank Simulator Integration**: Connects to a third-party simulated acquiring bank to ascertain authorized or declined statuses while gracefully handling errors if the bank becomes unavailable.
- **Secure Handling**: Exposes only the last four digits of a customer's card when payments are processed or retrieved.
- **Extensible Architecture**: Built on clean programming principles to guarantee high maintainability.

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **API Documentation**: SpringDoc OpenAPI UI (Swagger)
- **Testing**: JUnit 5, Mockito, Spring Boot MockMvc (for comprehensive integration testing)

## Setup & Running the Application

### Prerequisites

- Java 21
- Docker (for the Bank Simulator)
- Maven (or use the provided Maven wrapper `./mvnw`)

### 1. Start the Bank Simulator

The `Bank Simulator` must be running locally for the gateway to process payments. To start the simulator, run the following from the root of the project:

```bash
docker-compose up -d
```

It will be available at `http://localhost:8080/payments`.

### 2. Run the Gateway Server

You can run the Spring Boot application using the provided maven wrapper:

```bash
./mvnw spring-boot:run
```

By default, the server will start on port `8090` (as configured in `application.properties`).

### 3. Run the Tests

To run the full suite of unit and integration tests:

```bash
./mvnw test
```

## API Documentation (Swagger)

Once the application is running, you can interact with the API endpoints via the Swagger UI available at:

[http://localhost:8090/swagger-ui/index.html](http://localhost:8090/swagger-ui/index.html)

## Design Decisions & Assumptions

- **Separation of Concerns and Extensibility**: 
  - Controllers only handle web interactions, delegating business logic to the service layer.
  - The Service Layer operates upon the Dependency Inversion Principle, explicitly using a `PaymentGatewayService` interface implemented by `PaymentGatewayServiceImpl` to comfortably allow future extensions of alternative payment processors without altering core behavior.
  - A dedicated `BankSimulatorClient` wraps interactions with the external simulator.
- **In-Memory Storage**: 
  - Submissions are intentionally persisted in memory via `ConcurrentHashMap` in the `PaymentsRepository` to mimic behavior safely without adding the heavy-handed complexity of an external RDBMS integration, satisfying the test parameters.
- **Custom Validation rules**: 
  - Annotations (`@AllowedCurrency` and `@FutureExpiry`) act seamlessly with Hibernate Validator for clean domain model validity.
- **Centralized Error Handling**: 
  - A `@RestControllerAdvice` (`GlobalExceptionHandler`) efficiently handles `MethodArgumentNotValidException`, `BankUnavailableException`, and `PaymentNotFoundException`, returning strongly-typed `ErrorResponse` models mapping fields dynamically instead of fragile, disparate try-catches.
- **Testing Strategy**: 
  - Isolated Unit test boundaries alongside sprawling mock-based Integration Tests focusing intensely on happy-path completion, granular validation failures, boundary card lengths, and failure conditions.

## Bank Simulator Card Data

When testing the API endpoints, remember the simulator logic depending on the last digit of the Card Number:
- **1, 3, 5, 7, 9 (Odd)**: Authorized response (`200 OK`)
- **2, 4, 6, 8 (Even)**: Declined response (`200 OK`)
- **0**: Error simulating failure (`503 Service Unavailable`)
