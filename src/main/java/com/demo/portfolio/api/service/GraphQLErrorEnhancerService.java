package com.demo.portfolio.api.service;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that post-processes the raw GraphQL specification map returned by
 * {@code ExecutionResult.toSpecification()} and enriches validation errors
 * related to invalid enum values.
 *
 * <h2>Problem</h2>
 * <p>
 * GraphQL variable-coercion errors (e.g. an invalid {@code OrderStatus} value)
 * are produced by the {@code graphql-java} runtime <em>before</em> any data
 * fetcher runs. The default message looks like:
 * </p>
 * <pre>{@code
 * "Variable 'input' has an invalid value: Invalid input for enum 'OrderStatus'.
 *  No value found for name 'SxHIPPED'"
 * }</pre>
 * <p>It does not tell the caller which values <em>are</em> valid.</p>
 *
 * <h2>Solution</h2>
 * <p>
 * This service scans every error in the specification map, uses a regex to
 * detect the enum-validation pattern, looks up the matching
 * {@link GraphQLEnumType} in the live schema via {@link GraphQlSource}, and
 * rewrites the message to include the full list of valid values:
 * </p>
 * <pre>{@code
 * "Invalid value 'SxHIPPED' for enum 'OrderStatus'.
 *  Valid values are: [CANCELLED, DELIVERED, PENDING, PROCESSING, SHIPPED]"
 * }</pre>
 *
 * <p>
 * All other errors are returned unmodified so existing error handling is not
 * affected.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLErrorEnhancerService {

    /**
     * Matches the graphql-java enum-coercion error message.
     * <ul>
     *   <li>Group 1: enum type name (e.g. {@code OrderStatus})</li>
     *   <li>Group 2: the invalid value submitted by the caller (e.g. {@code SxHIPPED})</li>
     * </ul>
     */
    private static final Pattern ENUM_ERROR_PATTERN =
            Pattern.compile("Invalid input for enum '(\\w+)'\\. No value found for name '(\\w+)'");

    private final GraphQlSource graphQlSource;

    /**
     * Enriches enum validation errors in the given GraphQL specification map.
     *
     * <p>
     * The method makes a shallow copy of the top-level map so the original
     * {@code ExecutionResult} is not mutated. Each error entry whose message
     * matches {@link #ENUM_ERROR_PATTERN} is replaced with an enriched version
     * that includes the valid enum values. All other entries are kept as-is.
     * </p>
     *
     * @param spec the raw specification map produced by
     *             {@code ExecutionResult.toSpecification()}; must not be
     *             {@code null}
     * @return a new map identical to {@code spec} except that enum validation
     *         error messages are enriched with the list of valid values
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> enhance(Map<String, Object> spec) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) spec.get("errors");
        if (errors == null || errors.isEmpty()) {
            return spec;
        }

        List<Map<String, Object>> enhanced = errors.stream()
                .map(this::enhanceError)
                .toList();

        Map<String, Object> result = new HashMap<>(spec);
        result.put("errors", enhanced);
        return result;
    }

    /**
     * Enriches a single error entry if it matches the enum-validation pattern.
     *
     * <p>
     * If the error message does not match, or if the enum type cannot be found
     * in the schema, the original error map is returned unchanged.
     * </p>
     *
     * @param error a single error entry from the specification map; the
     *              implementation reads the {@code "message"} key and, when
     *              enhanced, writes a new {@code "message"} key
     * @return either the original {@code error} map or a shallow copy with a
     *         rewritten {@code "message"} value
     */
    private Map<String, Object> enhanceError(Map<String, Object> error) {
        String message = (String) error.get("message");
        if (message == null) {
            return error;
        }

        Matcher matcher = ENUM_ERROR_PATTERN.matcher(message);
        if (!matcher.find()) {
            return error;
        }

        String enumTypeName = matcher.group(1);
        String invalidValue  = matcher.group(2);

        GraphQLSchema schema = graphQlSource.schema();
        GraphQLType type = schema.getType(enumTypeName);
        if (!(type instanceof GraphQLEnumType enumType)) {
            log.debug("Enum type '{}' not found in schema; returning original error message", enumTypeName);
            return error;
        }

        List<String> validValues = enumType.getValues().stream()
                .map(GraphQLEnumValueDefinition::getName)
                .sorted()
                .toList();

        String enrichedMessage = String.format(
                "Invalid value '%s' for enum '%s'. Valid values are: %s",
                invalidValue, enumTypeName, validValues);

        log.debug("Enhanced enum error: {}", enrichedMessage);

        Map<String, Object> enriched = new HashMap<>(error);
        enriched.put("message", enrichedMessage);
        return enriched;
    }
}
