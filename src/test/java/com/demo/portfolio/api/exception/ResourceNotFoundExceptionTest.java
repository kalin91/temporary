package com.demo.portfolio.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceNotFoundExceptionTest {

    @Test
    void constructorSetsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");

        assertEquals("not found", ex.getMessage());
    }
}
