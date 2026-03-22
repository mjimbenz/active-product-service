# Active Product Service

Active Product Service is a microservice designed with a cloud-native, reactive, and domain-oriented architecture that manages the active products a bank customer may have.

This service operates exclusively on:
- Personal loans
- Business loans
- Credit cards

## 🧠 Purpose

- Manage the complete lifecycle of active products.

- Validate banking business rules.

- Integrate with Customer Service to validate customers.

- Maintain audit trails and perform soft deletes.

- Expose reactive and resilient APIs to the ecosystem.

## 🧩 Main Features

- **Spring Boot 3 + WebFlux**
- **Contract-First (OpenAPI 3 + openapi-generator)**
- **Reactive MongoDB**
- **RxJava in controllers (delegate pattern)**
- **Resilience4j**
- **Soft delete**
- **Structured JSON logging (Logstash + MDC)**
- **Clean architecture (DDD-lite)**

## 📐 Domain Architecture

The service implements the following key rules:

### ✔ Personal Credit
A personal customer can only have **one** active personal credit.

### ✔ Business Credit
A business can have **multiple** business credits.

### ✔ Credit Card
A credit card:
- requires `creditLimit > 0`
- initial balance = available credit
- transaction transactions are managed in **Movement Service**

### ✔ Bank Independence
A customer can have active products **without having bank accounts**.

## 🌐 API Contract (OpenAPI)

The `active-product-service-api.yaml` file defines:
- CRUD operations for active products
- Filters by customerId
- Auditing
- Soft delete

## 🔧 Technologies

- Java 17
- Spring Boot 3
- WebFlux
- Reactive MongoDB
- RxJava 3
- Resilience4j
- Logstash Logging
- OpenAPI Generator

## 📦 Run the project

```bash
mvn clean install
mvn spring-boot:run