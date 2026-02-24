package com.demo.portfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing an incoming GraphQL HTTP request body.
 *
 * <p>Used exclusively by the {@code /model} endpoint to receive
 * the GraphQL query string, optional named variables, and an
 * optional operation name (for documents with multiple operations).</p>
 *
 * @see com.demo.portfolio.api.fetcher.GraphQLModelController
 * @see com.demo.portfolio.api.service.QuerySanitizerService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLRequest {

    /**
     * The raw GraphQL query or mutation string.
     * Must not be blank; validated {@link com.demo.portfolio.api.service.QuerySanitizerService}
     * before execution.
     */
    @NotBlank(message = "GraphQL query must not be blank")
    private String query;

    /**
     * Named variables referenced inside the query (e.g. {@code $id: ID!}).
     * May be {@code null} or empty if the query has no variable references.
     */
    private Map<String, Object> variables;

    /**
     * The name of the operation to execute when the query document
     * contains more than one named operation. May be {@code null}.
     */
    private String operationName;
}
