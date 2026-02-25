package com.demo.portfolio.api.exception;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalDataFetcherExceptionHandlerTest {

    @Mock
    private DataFetcherExceptionHandlerParameters params;

    private final GlobalDataFetcherExceptionHandler handler = new GlobalDataFetcherExceptionHandler();

    @Test
    void handleExceptionMapsNotFound() {
        when(params.getException()).thenReturn(new ResourceNotFoundException("missing"));
        when(params.getPath()).thenReturn(null);

        DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

        assertEquals("missing", result.getErrors().getFirst().getMessage());
    }

    @Test
    void handleExceptionMapsSanitizationException() {
        when(params.getException()).thenReturn(new QuerySanitizationException("bad query"));
        when(params.getPath()).thenReturn(null);

        DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

        assertEquals("bad query", result.getErrors().getFirst().getMessage());
    }

    @Test
    void handleExceptionMapsIllegalArgument() {
        when(params.getException()).thenReturn(new IllegalArgumentException("bad arg"));
        when(params.getPath()).thenReturn(null);

        DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

        assertEquals("bad arg", result.getErrors().getFirst().getMessage());
    }

    @Test
    void handleExceptionMapsGenericException() {
        when(params.getException()).thenReturn(new RuntimeException("boom"));
        when(params.getPath()).thenReturn(null);

        DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

        assertEquals("Internal server error", result.getErrors().getFirst().getMessage());
    }
}
