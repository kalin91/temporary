package com.demo.portfolio.api.exception;

/**
 * Exception thrown to indicate that a requested resource could not be found in the system.
 * <p>
 * This exception is typically used in service and repository layers to signal that an entity
 * with a given identifier or criteria does not exist. It is intended to be caught by global
 * exception handlers to return a meaningful error response to the client, such as a 404 Not Found
 * in REST APIs or a GraphQL error with a NOT_FOUND code.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CustomerEntity customer = customerRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This exception is immutable and thread-safe.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ResourceNotFoundException} with the specified detail message.
     *
     * @param message the detail message explaining which resource was not found; must not be {@code null}
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
