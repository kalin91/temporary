package com.demo.portfolio.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuerySanitizationExceptionTest {

    @Test
    void constructorSetsMessage() {
        QuerySanitizationException ex = new QuerySanitizationException("invalid query");

        assertEquals("invalid query", ex.getMessage());
    }
}
