package com.demo.portfolio.api.exception;

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Global exception handler for GraphQL data fetchers.
 */
@Component
public class GlobalDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

    private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        
        if (exception instanceof ResourceNotFoundException) {
            TypedGraphQLError graphqlError = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();
            
            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                            .error(graphqlError)
                            .build()
            );
        }

        return defaultHandler.handleException(handlerParameters);
    }
}
