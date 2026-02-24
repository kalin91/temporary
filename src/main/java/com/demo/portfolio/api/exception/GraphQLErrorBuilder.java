package com.demo.portfolio.api.exception;

import java.util.List;
import java.util.Map;

/**
 * Utility class for building standardized GraphQL error response bodies.
 *
 * <p>Every error produced by this helper follows the same envelope:</p>
 * <pre>{@code
 * {
 *   "errors": [
 *     {
 *       "message": "...",
 *       "extensions": {
 *         "code": "BAD_REQUEST",
 *         "httpStatus": 400
 *       }
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>This ensures a single, consistent contract for API consumers regardless
 * of whether the error originates in the controller layer (sanitization),
 * the DGS data-fetcher layer, or a global exception handler.</p>
 *
 * @see GlobalDataFetcherExceptionHandler
 * @see com.demo.portfolio.api.fetcher.GraphQLModelController
 */
public final class GraphQLErrorBuilder {

    /** Private constructor — utility class. */
    private GraphQLErrorBuilder() {
    }

    /**
     * Builds a standard GraphQL error response body containing a single error entry.
     *
     * @param message    the human-readable error description
     * @param code       the machine-readable error code (e.g. {@code "BAD_REQUEST"},
     *                   {@code "NOT_FOUND"}, {@code "INTERNAL_ERROR"})
     * @param httpStatus the numeric HTTP status code that accompanies the error
     *                   (e.g. 400, 404, 500)
     * @return an immutable {@link Map} ready to be serialized as the JSON response body
     */
    public static Map<String, Object> buildErrorBody(String message, String code, int httpStatus) {
        return Map.of(
                "errors", List.of(
                        Map.of(
                                "message", message,
                                "extensions", Map.of(
                                        "code", code,
                                        "httpStatus", httpStatus
                                )
                        )
                )
        );
    }
}
