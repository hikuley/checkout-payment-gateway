# Checkout.com Payment Gateway

An API-based payment gateway built with Spring Boot 3.x and Java 21 to process card payments through a simulated acquiring bank.

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

### 3. Run Tests
```bash
./mvnw test
```

## API Documentation
Once running, the Swagger UI is available at:  
[http://localhost:8090/swagger-ui/index.html](http://localhost:8090/swagger-ui/index.html)

## Endpoints

* **POST `/api/payments`**: Initiates a new payment.
  * *Headers*: `X-Idempotency-Key` (Optional, to prevent duplicate requests).
* **GET `/api/payments/{id}`**: Retrieves a previously made payment by UUID (returning masked card details).
