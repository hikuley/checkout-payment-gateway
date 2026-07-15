# Checkout.com Payment Gateway

An API-based payment gateway built with Spring Boot 4.1.0 and Java 21 to process card payments through a simulated acquiring bank.

## Tech Stack

| Technology | Purpose | Version |
|---|---|---|
| Java | Language | 21 |
| Spring Boot | Application framework | 4.1.0 |
| Spring MVC | REST API layer | managed by Spring Boot |
| Spring Validation | Bean Validation (JSR-380) | managed by Spring Boot |
| springdoc-openapi | Swagger / OpenAPI UI | 2.8.6 |
| REST Assured | Integration test HTTP client | 5.5.0 |
| WireMock | HTTP mock server for tests | 3.10.0 |
| Maven | Build tool | (wrapper included) |

## Quick Start

### 1. Start the Bank Simulator
```bash
docker-compose up -d
```

### 2. Run the Application
```bash
./mvnw spring-boot:run
```
The server starts on port `8090` by default.

### 3. Run All Tests
To execute all the tests (both unit and integration tests) in the project, run:
```bash
./mvnw clean test
```

## API Documentation
Once running, the Swagger UI is available at:  
[http://localhost:8090/swagger-ui/index.html](http://localhost:8090/swagger-ui/index.html)

## Endpoints

* **POST `/api/payments`**: Initiates a new payment.
  * _Headers_: `X-Idempotency-Key` (Optional, to prevent duplicate requests).
* **GET `/api/payments/{id}`**: Retrieves a previously made payment by UUID (returning masked card details).

## Usage Examples

### 1. Happy Path Payment Request

**Request**
```http
POST http://localhost:8090/api/payments
Content-Type: application/json
X-Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000

{
  "cardNumber": "2222405343248877",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 1050,
  "cvv": "123"
}
```

**Response (`201 Created`)**
```json
{
  "id": "8b375b48-18e3-4de7-91a5-8e7cffc82bbd",
  "status": "Authorized",
  "cardNumberLastFour": "8877",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": 1050
}
```

### 2. Non-Happy Path (Validation Failure)

If invalid request data is sent (e.g., negative amount, invalid CVV format):

**Request**
```http
POST http://localhost:8090/api/payments
Content-Type: application/json

{
  "cardNumber": "2222405343248877",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "currency": "GBP",
  "amount": -50,
  "cvv": "ab"
}
```

**Response (`400 Bad Request`)**
```json
{
  "message": "Validation failed",
  "errors": {
    "amount": "Amount must be a positive integer",
    "cvv": "CVV must be 3 or 4 characters"
  }
}
```
