package com.demo.portfolio.api.exception;

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Global exception handler for all GraphQL data-fetcher errors.
 *
 * <p>Intercepts exceptions thrown during query/mutation execution and
 * converts them into structured GraphQL errors with a consistent
 * {@code extensions} block:</p>
 * <pre>{@code
 * {
 *   "message": "...",
 *   "extensions": {
 *     "code": "NOT_FOUND",
 *     "httpStatus": 404
 *   }
 * }
 * }</pre>
 *
 * <p>Handled exception types:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} &rarr; {@code NOT_FOUND / 404}</li>
 *   <li>{@link QuerySanitizationException} &rarr; {@code BAD_REQUEST / 400}</li>
 *   <li>{@link IllegalArgumentException} &rarr; {@code BAD_REQUEST / 400}</li>
 *   <li>Any other {@link Throwable} &rarr; {@code INTERNAL_ERROR / 500}</li>
 * </ul>
 *
 * @see GraphQLErrorBuilder
 */
@Slf4j
@Component
public class GlobalDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

    private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

    /**
     * Handles an exception raised by a DGS data fetcher and returns a
     * structured GraphQL error with {@code extensions.code} and
     * {@code extensions.httpStatus}.
     *
     * @param handlerParameters context provided by the DGS framework,
     *                          including the original exception and the
     *                          field path where it occurred
     * @return a {@link CompletableFuture} containing the error result
     */
    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {

        Throwable exception = handlerParameters.getException();

        if (exception instanceof ResourceNotFoundException) {
            log.warn("Resource not found during data fetch: {}", exception.getMessage());
            return buildResult(handlerParameters, exception.getMessage(), "NOT_FOUND", 404);
        }

        if (exception instanceof QuerySanitizationException) {
            log.warn("Query sanitization error during data fetch: {}", exception.getMessage());
            return buildResult(handlerParameters, exception.getMessage(), "BAD_REQUEST", 400);
        }

        if (exception instanceof IllegalArgumentException) {
            log.warn("Invalid argument during data fetch: {}", exception.getMessage());
            return buildResult(handlerParameters, exception.getMessage(), "BAD_REQUEST", 400);
        }

        log.error("Unexpected error during data fetch", exception);
        return buildResult(handlerParameters, "Internal server error", "INTERNAL_ERROR", 500);
    }

    /**
     * Builds a {@link DataFetcherExceptionHandlerResult} containing a single
     * {@link TypedGraphQLError} with the standard {@code extensions} map.
     *
     * @param params     the handler parameters (provides the field path)
     * @param message    the human-readable error message
     * @param code       the machine-readable error code
     * @param httpStatus the numeric HTTP status code
     * @return a completed future with the error result
     */
    private CompletableFuture<DataFetcherExceptionHandlerResult> buildResult(
            DataFetcherExceptionHandlerParameters params,
            String message,
            String code,
            int httpStatus) {

        TypedGraphQLError graphqlError = TypedGraphQLError.newBuilder()
                .message(message)
                .path(params.getPath())
                .extensions(Map.of(
                        "code", code,
                        "httpStatus", httpStatus
                ))
                .build();

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult()
                        .error(graphqlError)
                        .build()
        );
    }
}
