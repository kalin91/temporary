package com.demo.portfolio.api.service;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that post-processes {@link ExecutionResult} objects returned by the
 * GraphQL engine and enriches validation errors related to invalid enum values.
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
 * This service scans every {@link GraphQLError} in the {@link ExecutionResult},
 * uses a regex to detect the enum-validation pattern, looks up the matching
 * {@link GraphQLEnumType} in the live schema via {@link GraphQlSource}, and
 * rebuilds the error with an enriched message that includes the full list of
 * valid values:
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
 *
 * @see com.demo.portfolio.api.config.ErrorEnhancerInterceptor
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
     * Enriches enum validation errors in the given {@link ExecutionResult}.
     *
     * <p>
     * If the result contains no errors, it is returned as-is. Otherwise, each
     * error whose message matches {@link #ENUM_ERROR_PATTERN} is replaced with
     * an enriched version that includes the valid enum values. A new
     * {@link ExecutionResult} is built with the enhanced error list; the
     * original data and extensions are preserved.
     * </p>
     *
     * @param result the {@link ExecutionResult} produced by the GraphQL engine;
     *               must not be {@code null}
     * @return the original result if no errors are present, or a new
     *         {@link ExecutionResult} with enriched enum error messages
     */
    public ExecutionResult enhance(ExecutionResult result) {
        List<GraphQLError> errors = result.getErrors();
        if (errors == null || errors.isEmpty()) {
            return result;
        }

        List<GraphQLError> enhanced = errors.stream()
                .map(this::enhanceError)
                .toList();

        return ExecutionResultImpl.newExecutionResult()
                .data(result.getData())
                .errors(enhanced)
                .extensions(result.getExtensions())
                .build();
    }

    /**
     * Enriches a single {@link GraphQLError} if it matches the enum-validation
     * pattern.
     *
     * <p>
     * If the error message does not match, or if the enum type cannot be found
     * in the schema, the original error is returned unchanged.
     * </p>
     *
     * @param error a single {@link GraphQLError} from the execution result
     * @return either the original error or a new {@link GraphQLError} with an
     *         enriched message that includes valid enum values
     */
    private GraphQLError enhanceError(GraphQLError error) {
        String message = error.getMessage();
        if (message == null) {
            return error;
        }

        Matcher matcher = ENUM_ERROR_PATTERN.matcher(message);
        if (!matcher.find()) {
            return error;
        }

        String enumTypeName = matcher.group(1);
        String invalidValue = matcher.group(2);

        GraphQLSchema schema = graphQlSource.schema();
        GraphQLType type = schema.getType(enumTypeName);
        if (!(type instanceof GraphQLEnumType enumType)) {
            log.debug("Enum type '{}' not found in schema; returning original error", enumTypeName);
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

        return GraphqlErrorBuilder.newError()
                .message(enrichedMessage)
                .locations(error.getLocations())
                .path(error.getPath())
                .extensions(error.getExtensions())
                .errorType(error.getErrorType())
                .build();
    }
}
