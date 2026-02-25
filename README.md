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

- GraphQL API with a single endpoint (`/model`)
- Basic CRUD operations for `Customers` and `Orders`
- Pagination and filtering support
- Input validation and global exception handling
- Structured logging and Actuator health endpoints
- H2 Console accessible at `http://localhost:8082` (configured for WebFlux)

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

```bash
./gradlew test
```

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
