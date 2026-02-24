# GitHub Copilot Instructions for backend-portfolio-api

This file provides context and guidelines for AI agents working on this project. The project is a production-style demo API intended as a backend portfolio piece, demonstrating professional engineering practices.

## Project Overview
- **Type**: Netflix DGS GraphQL API (Single Endpoint)
- **Purpose**: Backend portfolio and professional demonstration.

## Technology Stack
- **Languages**: Java 21
- **Framework**: Spring Boot 3.4.13
- **Build Tool**: Gradle 8.14.3
- **Reactive Stack**: Spring WebFlux (Project Reactor)
- **GraphQL**: Netflix DGS Framework
- **Persistence**: Spring Data JPA with Hibernate
- **Database**: H2 Database (In-Memory for demo, accessible via console)
- **Containerization**: Docker

## Architecture Guidelines
The application must follow a strict layered architecture:
1.  **Controller Layer (GraphQL Data Fetchers)**: Handles GraphQL requests and responses.
2.  **Facade Layer (Optional)**: Orchestrates transactions and complex business flows.
3.  **Service Layer**: Contains business logic.
4.  **Repository Layer**: Handles data access using Spring Data JPA.

### Key Architectural Rules
- **Reactive Types**: Use `Mono` and `Flux` consistently throughout the application (WebFlux).
- **No Entities in API**: NEVER expose JPA Entities directly in the GraphQL schema. Always use DTOs.
- **Mapping**: Use MapStruct or explicit manual mappers to convert between Entities and DTOs.
- **Clean Code**: Follow SOLID principles.
- **Cloud-Ready**: Design for statelessness and containerization.

## Functional Requirements
- **Domains**: Implement sample domains including **Orders** and **Customers**.
- **CRUD**: Implement basic Create, Read, Update, Delete operations.
- **Schema**: Maintain a well-defined `src/main/resources/schema/schema.graphqls` file.
- **Seed Data**: Provide SQL scripts or a CommandLineRunner to load sample seed data into H2 on startup.
- **Pagination & Filtering**: Implement basic pagination (Connection/Cursor or Offset based) and filtering for list queries.
- **Validation**: Include strict input validation (JSR-303/Jakarta Validation).
- **Error Handling**: Implement global exception handling to return meaningful GraphQL errors (DataFetchingExceptionHandler).
- **Observability**:
    -   **Logging**: Use structured logging (SLF4J).
    -   **Health**: Expose Spring Actuator health endpoints.

## Configuration & Coding Standards
- **Configuration**: Use `application.yml`. Ensure environment-specific settings are clear.
- **Package Structure**:
    -   `com.java.portfolio.api` (Root)
    -   `.domain` (Entities)
    -   `.dto` (Data Transfer Objects)
    -   `.fetcher` (GraphQL Data Fetchers)
    -   `.service`
    -   `.repository`
    -   `.mapper`
    -   `.config`
    -   `.exception`
- **Documentation**: 
    -   Add full comprehensive **JavaDoc** for all main classes and every methods that include a detailed description of parameters, return values, and exceptions.
    -   Ensure code is self-documenting where possible.
    -   During Code-Review, verify that all new and modified code includes appropriate JavaDoc and adheres to the project's documentation standards.

## Testing Strategy
- **Unit Tests**: Use JUnit 5 and Mockito for isolated definitions.
- **Integration Tests**: Test GraphQL execution logic using `@DgsQueryExecutor` or similar integration test utilities provided by DGS.

## Deliverables
- **Dockerfile**: Minimal image for running the application.
- **README.md**: Comprehensive instructions on building, running, and testing the application.

## Quality Statement
The project should demonstrate **clean architecture**, **cloud-ready design**, and **professional backend engineering practices**.
