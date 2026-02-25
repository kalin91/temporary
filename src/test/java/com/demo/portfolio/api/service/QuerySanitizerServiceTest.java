package com.demo.portfolio.api.service;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.exception.QuerySanitizationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuerySanitizerServiceTest {

    private final QuerySanitizerService service = new QuerySanitizerService();

    @Test
    void sanitizeTrimsAndReturnsRequest() {
        GraphQLRequest input = GraphQLRequest.builder().query("  query { customers { id } }  ").operationName("op").build();

        GraphQLRequest result = service.sanitize(input).block();

        assertNotNull(result);
        assertEquals("query { customers { id } }", result.getQuery());
        assertEquals("op", result.getOperationName());
    }

    @Test
    void sanitizeRejectsBlankQuery() {
        GraphQLRequest input = GraphQLRequest.builder().query(" ").build();

        assertThrows(QuerySanitizationException.class, () -> service.sanitize(input).block());
    }

    @Test
    void sanitizeRejectsOversizedQuery() {
        String big = "query { " + "a".repeat(10001) + " }";
        GraphQLRequest input = GraphQLRequest.builder().query(big).build();

        assertThrows(QuerySanitizationException.class, () -> service.sanitize(input).block());
    }

    @Test
    void sanitizeRejectsDangerousPattern() {
        GraphQLRequest input = GraphQLRequest.builder().query("query { customers(filter:\"<script>\") { id } }").build();

        assertThrows(QuerySanitizationException.class, () -> service.sanitize(input).block());
    }

    @Test
    void sanitizeRejectsInvalidSyntax() {
        GraphQLRequest input = GraphQLRequest.builder().query("query { customers { id }").build();

        assertThrows(QuerySanitizationException.class, () -> service.sanitize(input).block());
    }

    @Test
    void sanitizeRejectsDeepNesting() {
        String query = "query { a { b { c { d { e { f { g { h { i { j { k } } } } } } } } } } }";
        GraphQLRequest input = GraphQLRequest.builder().query(query).build();

        assertThrows(QuerySanitizationException.class, () -> service.sanitize(input).block());
    }
}
