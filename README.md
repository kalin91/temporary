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
- **Spring Security** (HTTP Basic Auth + method-level authorization)
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
- **Security**: HTTP Basic Auth + role-based access control on every GraphQL operation. Query complexity analysis guards against resource-exhaustion attacks.
- **Pagination**: Standard offset-based pagination strategy.
- **Error Handling**: Global exception handling producing standard GraphQL errors with extended metadata.
- **Observability**: Structured logging and Actuator health endpoints.
- **Tools**: H2 Console accessible at `http://localhost:8082`.

## Security

### Overview

Every GraphQL operation is protected by **HTTP Basic Auth**. The API enforces role-based authorization at the method level using Spring Security's `@PreAuthorize` annotation on DGS data fetcher methods. This means authorization is checked *after* authentication, giving precise per-operation control.

Two CodeQL `spring-disabled-csrf-protection` alerts are intentional false positives — the API is fully stateless and does not issue session cookies, so CSRF attacks cannot be mounted against it.

### Role Profiles

Three role profiles are available. Roles are cumulative: `admin` holds all three roles.

| Profile  | Spring Roles Granted                   | Permitted Operations                        |
|----------|----------------------------------------|---------------------------------------------|
| `reader` | `ROLE_READER`                          | Read-only queries (`customers`, `customer`, `orders`, `order`) |
| `writer` | `ROLE_WRITER`, `ROLE_READER`           | All reads + `createCustomer`, `updateCustomer`, `createOrder`, `updateOrder` |
| `admin`  | `ROLE_ADMIN`, `ROLE_WRITER`, `ROLE_READER` | All reads + all writes + `deleteCustomer`, `deleteOrder` |

Public paths (no authentication required):

- `GET /actuator/health` and `GET /actuator/info` — liveness/readiness probes
- `GET /graphiql/**` — in-browser GraphQL IDE (development convenience)

### Credential Configuration

Credentials are loaded from the `API_CREDENTIALS_JSON` environment variable at startup. The variable must be a JSON object with three keys (`admin`, `writer`, `reader`), each carrying a `user`/`pass`/`permissions` triple:

```json
{
  "admin":  { "user": "api_admin",  "pass": "<strong-secret>", "permissions": 7 },
  "writer": { "user": "api_writer", "pass": "<strong-secret>", "permissions": 6 },
  "reader": { "user": "api_reader", "pass": "<strong-secret>", "permissions": 4 }
}
```

The `permissions` field is an additive bitmask: `ADMIN=1`, `WRITER=2`, `READER=4`. A value of `7` (= 1+2+4) grants all three roles; `6` (= 2+4) grants writer + reader; `4` grants reader only.

If `API_CREDENTIALS_JSON` is not set, the application falls back to local-dev defaults defined in `application.yml`. **Never use the defaults in production.**

#### Example: starting with custom credentials

```bash
export API_CREDENTIALS_JSON='{"admin":{"user":"api_admin","pass":"s3cr3t!","permissions":7},"writer":{"user":"api_writer","pass":"wr1t3!","permissions":6},"reader":{"user":"api_reader","pass":"r3@d!","permissions":4}}'
./gradlew bootRun
```

#### Example: calling the API with Basic Auth

```bash
# Read-only query as reader
curl -u api_reader:reader123 -X POST http://localhost:8080/model \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ customers(page:0,size:5) { content { id firstName } } }"}'

# Create a customer as writer
curl -u api_writer:writer123 -X POST http://localhost:8080/model \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createCustomer(input:{firstName:\"Jane\",lastName:\"Doe\",email:\"jane@example.com\"}) { id } }"}'

# Delete a customer as admin
curl -u api_admin:admin123 -X POST http://localhost:8080/model \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { deleteCustomer(id:\"1\") }"}'
```

### Permission Bitmask (`Permission` enum)

The `Permission` enum in `com.demo.portfolio.api.config` encodes each permission level as a single bit. This allows a credential JSON entry to declare its roles via a compact integer field rather than a list of strings. The `SecurityConfig` converts each entry's bitmask into Spring `GrantedAuthority` values at startup using `Permission.fromMask(int)`.

```
ADMIN  = 1  → ROLE_ADMIN  (deleteCustomer, deleteOrder)
WRITER = 2  → ROLE_WRITER (createCustomer, updateCustomer, createOrder, updateOrder)
READER = 4  → ROLE_READER (customers, customer, orders, order)
```

The enum also defines SpEL expression constants (`ROLE_ADMIN`, `ROLE_WRITER`, `ROLE_READER`) that are used directly as `@PreAuthorize` annotation values, keeping the authorization logic DRY and refactoring-safe.

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

> **Note:** When running locally without setting `API_CREDENTIALS_JSON`, the application uses the built-in development defaults (`api_admin` / `admin123`, `api_writer` / `writer123`, `api_reader` / `reader123`). GraphiQL requires these credentials too — use the **HTTP Headers** pane to set `Authorization: Basic YXBpX2FkbWluOmFkbWluMTIz` (base64 of `api_admin:admin123`).

### Running Tests

The project employs a comprehensive testing strategy combining unit and integration tests.

#### **1. Unit Testing (JUnit 5 & Mockito)**

- **Scope**: Internal business logic, service layer, and configuration classes.
- **Command**: `./gradlew test` (skips Karate tests)

#### **2. Integration Testing (Karate)**

- **Scope**: End-to-end API testing — GraphQL queries, mutations, H2 database, schema validation, and access-control enforcement.
- **Features**: Parallel execution, full request/response logging, schema-first validation.
- **Auth**: Each scenario sends an explicit `Authorization: Basic …` header via the `authHeader('role')` helper defined in `karate-config.js`.
- **Command**: `./gradlew karateTest`

> **Integration tests with custom credentials**: If you set `API_CREDENTIALS_JSON` for the app, export the same variable before running Karate so `karate-config.js` picks it up and generates the correct headers.

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
docker run -p 8080:8080 -p 8082:8082 \
  -e API_CREDENTIALS_JSON='{"admin":{"user":"api_admin","pass":"s3cr3t","permissions":7},"writer":{"user":"api_writer","pass":"wr1t3","permissions":6},"reader":{"user":"api_reader","pass":"r3@d","permissions":4}}' \
  backend-portfolio-api
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
