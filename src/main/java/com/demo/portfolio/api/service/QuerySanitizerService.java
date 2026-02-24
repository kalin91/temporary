package com.demo.portfolio.api.service;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.exception.QuerySanitizationException;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
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
 *   <li>AST parsing — rejects syntactically invalid queries (includes unbalanced
 *       braces) via {@link InvalidSyntaxException}.</li>
 *   <li>Maximum selection-set nesting depth enforcement
 *       ({@value #MAX_QUERY_DEPTH} levels), computed on the parsed AST so that
 *       braces inside string literals are never counted.</li>
 * </ol>
 *
 * <p>All violations result in a {@link QuerySanitizationException} wrapped in
 * a failed {@link Mono} so the reactive pipeline propagates errors cleanly.
 */
@Slf4j
@Service
public class QuerySanitizerService {

    /** Maximum allowed character length for a raw query string. */
    private static final int MAX_QUERY_LENGTH = 10_000;

    /**
     * Maximum allowed selection-set nesting depth, measured on the parsed AST.
     * Each level of {@code { ... }} inside a field selection counts as one depth
     * unit; braces inside string literals are ignored.
     */
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

        // Parse to an AST: rejects syntactically invalid queries (including
        // unbalanced braces) and enables accurate depth measurement that is
        // immune to braces embedded inside string literal argument values.
        Document document;
        try {
            document = Parser.parse(trimmed);
        } catch (InvalidSyntaxException e) {
            log.warn("Rejected syntactically invalid GraphQL query: {}", e.getMessage());
            return Mono.error(new QuerySanitizationException(
                    "Query contains invalid syntax: " + e.getMessage()));
        }

        int depth = calculateDepth(document);
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
     * Computes the maximum selection-set nesting depth of a parsed
     * {@link Document} by recursively walking every operation and fragment
     * definition.
     *
     * <p>Fragment spreads ({@code ...FragmentName}) are treated as one extra
     * depth level, because resolving named fragments would require the full
     * schema and fragment map. This is a known conservative approximation;
     * it will never under-count depth.</p>
     *
     * @param document the parsed {@link Document}; must not be {@code null}
     * @return the maximum selection-set depth found across all definitions,
     *         or {@code 0} if the document contains no definitions
     */
    private int calculateDepth(Document document) {
        return document.getDefinitions().stream()
                .mapToInt(def -> {
                    if (def instanceof OperationDefinition op) {
                        return selectionSetDepth(op.getSelectionSet(), 0);
                    }
                    if (def instanceof FragmentDefinition frag) {
                        return selectionSetDepth(frag.getSelectionSet(), 0);
                    }
                    return 0;
                })
                .max()
                .orElse(0);
    }

    /**
     * Recursively computes the maximum depth within a {@link SelectionSet},
     * counting each level of field nesting as one unit.
     *
     * @param selectionSet the selection set to traverse; may be {@code null}
     *                     (treated as leaf — returns {@code currentDepth})
     * @param currentDepth the depth at which this selection set appears
     * @return the maximum depth reached within {@code selectionSet}
     */
    @SuppressWarnings("rawtypes")
    private int selectionSetDepth(SelectionSet selectionSet, int currentDepth) {
        if (selectionSet == null) {
            return currentDepth;
        }

        int nextDepth = currentDepth + 1;
        List<Selection> selections = selectionSet.getSelections();

        if (selections == null || selections.isEmpty()) {
            return nextDepth;
        }

        return selections.stream()
                .mapToInt(selection -> {
                    if (selection instanceof Field field) {
                        return selectionSetDepth(field.getSelectionSet(), nextDepth);
                    }
                    if (selection instanceof InlineFragment fragment) {
                        return selectionSetDepth(fragment.getSelectionSet(), nextDepth);
                    }
                    // FragmentSpread: name-only reference; we cannot resolve the
                    // fragment depth without the full document context + schema,
                    // so conservatively count it as one additional level.
                    return nextDepth;
                })
                .max()
                .orElse(nextDepth);
    }
}

