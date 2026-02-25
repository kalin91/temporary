# Backend Portfolio API

This is a production-style demo API intended as a backend portfolio piece, demonstrating professional engineering practices.

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.11**
- **Gradle 8.14.3**
- **Spring WebFlux** (Reactive Stack)
- **Netflix DGS Framework** (GraphQL)
- **Spring Data JPA** with Hibernate
- **H2 Database** (In-Memory)
- **MapStruct** for DTO mapping
- **Docker** for containerization

## Architecture

The application follows a strict layered architecture:

1. **Controller Layer (GraphQL Data Fetchers)**: Handles GraphQL requests and responses.
2. **Service Layer**: Contains business logic and transaction management.
3. **Repository Layer**: Handles data access using Spring Data JPA.

Reactive types (`Mono` and `Flux`) are used consistently in the API layer, with blocking JPA calls wrapped appropriately using `Schedulers.boundedElastic()`.

## Features

- **GraphQL API**: Schema-first design with a single endpoint (`/model`).
- **Reactive Stack**: Built on Spring WebFlux, bridging blocking JPA repositories with reactive data fetchers.
- **N+1 Optimization**: Solved using `DataLoaders` with composite keys for argument-aware batching.
- **Security**: Query complexity analysis to prevent DoS attacks via deep/expensive queries.
- **Pagination**: Standard offset-based pagination strategy.
- **Error Handling**: Global exception handling producing standard GraphQL errors with extended metadata.
- **Observability**: Structured logging and Actuator health endpoints.
- **Tools**: H2 Console accessible at `http://localhost:8082`.

## Technical Highlights & Patterns

This project implements several advanced patterns to demonstrate professional backend engineering:

### 1. N+1 Problem Solver (Composite Key DataLoaders)

Instead of simple ID-based batching, this API uses a **Composite Record Key** (`OrdersByCustomerKey`) for DataLoaders. This allows batching child collections (Orders) while preserving arguments passed to the field (e.g., filtering orders by status *within* a batched call).

- *See:* `OrdersByCustomerDataLoader.java`

### 2. Query Complexity Instrumentation

To protect the server from resource exhaustion, the API implements a custom `Instrumentation` that calculates the "cost" of a query before execution.

- **Root Fields**: High cost.
- **Nested Lists**: Multiplied cost.
- **Limit**: Queries exceeding the budget (850 points) are rejected immediately.
- *See:* `GraphQLInstrumentationConfig.java`

### 3. Reactive-Blocking Bridge

The application runs on Netty (WebFlux) but uses Hibernate (Blocking). It bridges these worlds by wrapping blocking repository calls in `Mono.fromCallable` and subscribing on `Schedulers.boundedElastic()`, ensuring the event loop remains non-blocking.

### 4. Request Sanitization (Pre-Execution Validation)

Before any query reaches the execution engine, a `WebGraphQlInterceptor` validates and sanitizes the raw request payload. This adds a security layer against injection or malformed input at the gateway level.

- *See:* `SanitizingInterceptor.java`

### 5. Global Error Handling

Exceptions are not just thrown; they are intercepted and transformed into meaningful GraphQL errors.

- **TypedGraphQLError**: Returns specific error codes (e.g., `RESOURCE_NOT_FOUND`) and HTTP status suggestions in the `extensions` map.
- *See:* `GlobalDataFetcherExceptionHandler.java`

### 6. Type-Safe Mapping

Uses **MapStruct** for compile-time generation of mappers between JPA Entities and GraphQL DTOs, avoiding runtime reflection overhead.

## Getting Started

### Prerequisites

- Java 21
- Docker (optional)

### Building the Project

```bash
./gradlew build
```

### Running the Application

```bash
./gradlew bootRun
```

The application will start on port `8080`.
The GraphQL endpoint is available at `http://localhost:8080/model`.
GraphiQL (UI for testing queries) is available at `http://localhost:8080/graphiql`.

### Running Tests

The project employs a comprehensive testing strategy combining unit and integration tests.

#### **1. Unit Testing (JUnit 5 & Mockito)**

- **Scope**: Internal business logic & service layer.
- **Command**: `./gradlew test` (skips Karate tests)

#### **2. Integration Testing (Karate)**

- **Scope**: End-to-end API testing (GraphQL queries, H2 database, Schema validation).
- **Features**: Parallel execution, full request/response logging, schema-first validation.
- **Command**: `./gradlew karateTest`

#### **3. Full Suite & Reporting**

Run both suites and generate a combined HTML report:

```bash
./gradlew check
```

- **Report Location**: `build/reports/tests/merged/index.html`

### Docker

To build and run the application using Docker:

```bash
docker build -t backend-portfolio-api .
docker run -p 8080:8080 -p 8082:8082 backend-portfolio-api
```

## Sample Queries

### Get Customers with Pagination

```graphql
query GetCustomers($page: Int, $size: Int, $orderStatus: OrderStatus, $orderPage: Int, $orderSize: Int) {
  customers(page: $page, size: $size) {
    content {
      id
      firstName
      lastName
      email
      orders(status: $orderStatus, page: $orderPage, size: $orderSize) {
        id
        status
        totalAmount
      }
    }
    totalPages
    totalElements
    number
  }
}
```

```json
// variables
{
    "page": 0,
    "size": 100,
    "orderStatus": null,
    "orderPage": 0,
    "orderSize": 100
}
```

### Create an Order

```graphql
mutation {
  createOrder(input: {
    customerId: "1",
    totalAmount: 199.99
  }) {
    id
    status
    totalAmount
  }
}
```
