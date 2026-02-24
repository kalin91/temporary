package com.demo.portfolio.api.exception;

/**
 * Exception thrown when an incoming GraphQL query fails one or more
 * sanitization rules in {@link com.demo.portfolio.api.service.QuerySanitizerService}.
 *
 * <p>This exception is intended to be caught by the global exception handler
 * and translated into a meaningful HTTP 400 response before reaching the
 * DGS data fetchers.</p>
 */
public class QuerySanitizationException extends RuntimeException {

    /**
     * Constructs a new {@code QuerySanitizationException} with the given detail message.
     *
     * @param message a human-readable description of the violated sanitization rule
     */
    public QuerySanitizationException(String message) {
        super(message);
    }
}
