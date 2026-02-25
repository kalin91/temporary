package com.demo.portfolio.api.config;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that registers GraphQL execution instrumentations.
 *
 * <h2>Query Complexity Limit</h2>
 * <p>
 * {@link MaxQueryComplexityInstrumentation} assigns a numeric <em>cost</em> to each
 * requested field and rejects the query when the total cost exceeds
 * {@value #MAX_QUERY_COMPLEXITY}. This catches expensive-but-shallow queries that the
 * depth-based check in {@link com.demo.portfolio.api.service.QuerySanitizerService}
 * cannot detect.
 * </p>
 *
 * <h2>Field cost model</h2>
 * <ul>
 *   <li>{@code customers} / {@code orders} (connection root fields): {@code 10 + childComplexity × 10}
 *       — these may return large paginated sets.</li>
 *   <li>{@code content} (pagination list): {@code childComplexity}
 *       — structural wrapper for list items.</li>
 *   <li>Everything else (scalars, single-entity lookups): {@code 1 + childComplexity}.</li>
 * </ul>
 *
 * <h2>Example complexities</h2>
 * <pre>
 * customers { content { id } }                 → ~30   ✅ allowed
 * customers { content { orders { id } } }      → ~130  ✅ allowed
 * customers(size:100) { orders(size:100) { … } } → >200 ❌ rejected
 * </pre>
 *
 * <p>Spring GraphQL / DGS automatically detects all {@code Instrumentation} beans
 * registered in the application context; no additional wiring is required.</p>
 */
@Slf4j
@Configuration
public class GraphQLInstrumentationConfig {

    /**
     * Maximum allowed numeric complexity for a single GraphQL query.
     * Queries exceeding this threshold are rejected before execution with a
     * GraphQL error.
     */
    private static final int MAX_QUERY_COMPLEXITY = 850;

    /**
     * Registers a {@link MaxQueryComplexityInstrumentation} bean that enforces the
     * query complexity budget defined by {@value #MAX_QUERY_COMPLEXITY}.
     *
     * <p>The field-complexity calculator uses a weighted model:
     * <ul>
     *   <li>Collection root fields ({@code customers}, {@code orders}):
     *       {@code 10 + childComplexity × 10}</li>
     *   <li>Pagination structural fields ({@code content}):
     *       {@code childComplexity} (no overhead)</li>
     *   <li>All other fields: {@code 1 + childComplexity}</li>
     * </ul>
     * </p>
     *
     * @return the configured {@link MaxQueryComplexityInstrumentation} instance
     */
    @Bean
    public MaxQueryComplexityInstrumentation maxQueryComplexityInstrumentation() {
        return new MaxQueryComplexityInstrumentation(MAX_QUERY_COMPLEXITY, (env, childComplexity) -> {
            String fieldName = env.getField().getName();
            int cost = switch (fieldName) {
                // Connection root fields — each may expand N rows
                case "customers", "orders" -> 10 + childComplexity * 10;
                // Pagination structural fields — add no cost of their own
                case "content" -> childComplexity;
                // Scalars, single-entity lookups, and all other fields
                default -> 1 + childComplexity;
            };
            log.trace("Field '{}' complexity: {} (child={})", fieldName, cost, childComplexity);
            return cost;
        });
    }
}
