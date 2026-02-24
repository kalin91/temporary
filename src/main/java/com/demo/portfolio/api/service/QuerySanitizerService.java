package com.demo.portfolio.api.service;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.exception.QuerySanitizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

/**
 * Service responsible for sanitizing incoming GraphQL queries before
 * they are forwarded to the DGS data fetchers.
 *
 * <p>Sanitization rules applied (in order):
 * <ol>
 *   <li>Null / blank query rejection.</li>
 *   <li>Whitespace trimming.</li>
 *   <li>Maximum query length enforcement ({@value #MAX_QUERY_LENGTH} chars).</li>
 *   <li>Dangerous injection pattern detection (script tags, JS event handlers).</li>
 *   <li>Maximum nesting depth enforcement ({@value #MAX_QUERY_DEPTH} levels).</li>
 * </ol>
 *
 * <p>All violations result in a {@link QuerySanitizationException} wrapped in
 * a failed {@link Mono} so the reactive pipeline propagates errors cleanly.
 */
@Service
public class QuerySanitizerService {

    private static final Logger log = LoggerFactory.getLogger(QuerySanitizerService.class);

    /** Maximum allowed character length for a raw query string. */
    private static final int MAX_QUERY_LENGTH = 10_000;

    /** Maximum allowed brace-nesting depth inside a query. */
    private static final int MAX_QUERY_DEPTH = 10;

    /**
     * Pattern that matches common injection / XSS patterns that have no
     * legitimate place inside a GraphQL query string.
     *
     * <ul>
     *   <li>{@code <script} — HTML script tag injection.</li>
     *   <li>{@code javascript:} — inline JS protocol.</li>
     *   <li>{@code on\w+=} — HTML event-handler attributes.</li>
     *   <li>{@code --} — SQL comment sequences.</li>
     *   <li>{@code ;DROP|;INSERT|;UPDATE|;DELETE} — chained SQL statements.</li>
     * </ul>
     */
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?i)(<script|javascript:|on\\w+\\s*=|--|;\\s*(drop|insert|update|delete)\\s)"
    );

    /**
     * Sanitizes the incoming {@link GraphQLRequest}.
     *
     * <p>Returns a new {@link GraphQLRequest} with the trimmed query.
     * If any rule is violated the returned {@link Mono} terminates with a
     * {@link QuerySanitizationException}.</p>
     *
     * @param request the raw request received from the HTTP layer; must not be {@code null}
     * @return a {@link Mono} emitting the sanitized request, or an error {@link Mono}
     *         on any violation
     * @throws QuerySanitizationException (via {@link Mono#error}) when the query fails
     *         any sanitization rule
     */
    public Mono<GraphQLRequest> sanitize(GraphQLRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            log.warn("Rejected empty or null GraphQL query");
            return Mono.error(new QuerySanitizationException("Query must not be blank"));
        }

        String trimmed = request.getQuery().strip();

        if (trimmed.length() > MAX_QUERY_LENGTH) {
            log.warn("Rejected oversized query: {} chars (max {})", trimmed.length(), MAX_QUERY_LENGTH);
            return Mono.error(new QuerySanitizationException(
                    "Query exceeds maximum allowed length of " + MAX_QUERY_LENGTH + " characters"));
        }

        if (DANGEROUS_PATTERN.matcher(trimmed).find()) {
            log.warn("Rejected query containing dangerous pattern");
            return Mono.error(new QuerySanitizationException("Query contains forbidden patterns"));
        }

        int depth = calculateDepth(trimmed);
        if (depth > MAX_QUERY_DEPTH) {
            log.warn("Rejected deeply nested query: depth {} (max {})", depth, MAX_QUERY_DEPTH);
            return Mono.error(new QuerySanitizationException(
                    "Query nesting depth " + depth + " exceeds maximum of " + MAX_QUERY_DEPTH));
        }

        log.debug("Query passed sanitization (length={}, depth={})", trimmed.length(), depth);

        GraphQLRequest sanitized = GraphQLRequest.builder()
                .query(trimmed)
                .variables(request.getVariables())
                .operationName(request.getOperationName())
                .build();

        return Mono.just(sanitized);
    }

    /**
     * Calculates the maximum brace-nesting depth of a GraphQL query string
     * by counting {@code '{'} and {@code '}'} characters.
     *
     * <p>This is a lightweight approximation; it does not parse the full
     * GraphQL AST but is sufficient for depth-limit enforcement.</p>
     *
     * @param query the (already trimmed) query string
     * @return the maximum brace depth found
     */
    private int calculateDepth(String query) {
        int current = 0;
        int max = 0;
        for (char c : query.toCharArray()) {
            if (c == '{') {
                current++;
                if (current > max) max = current;
            } else if (c == '}') {
                current--;
            }
        }
        return max;
    }
}
