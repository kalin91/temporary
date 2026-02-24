package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.exception.GraphQLErrorBuilder;
import com.demo.portfolio.api.exception.QuerySanitizationException;
import com.demo.portfolio.api.service.GraphQLErrorEnhancerService;
import com.demo.portfolio.api.service.QuerySanitizerService;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux REST controller that exposes the single GraphQL entry point at
 * {@code POST /model}.
 *
 * <h2>Request / Response</h2>
 * <p>
 * Accepts a JSON body conforming to {@link GraphQLRequest} and returns a
 * standard GraphQL response envelope:
 * </p>
 * 
 * <pre>{@code
 * // request
 * POST /model
 * Content-Type: application/json
 * {
 *   "query":         "{ customers { edges { node { id firstName } } } }",
 *   "variables":     {},
 *   "operationName": null
 * }
 *
 * // success response
 * HTTP 200
 * { "data": { "customers": { ... } } }
 *
 * // sanitization error response
 * HTTP 400
 * { "errors": [ { "message": "Query contains forbidden patterns" } ] }
 * }</pre>
 *
 * <h2>Pipeline</h2>
 * <ol>
 * <li>The raw body is deserialized into a {@link GraphQLRequest}.</li>
 * <li>{@link QuerySanitizerService#sanitize(GraphQLRequest)} validates and
 * trims the query.</li>
 * <li>The sanitized query is forwarded to {@link DgsReactiveQueryExecutor}, which
 * returns a {@link Mono} natively — no blocking thread needed.</li>
 * <li>The {@code ExecutionResult} is converted to a standard GraphQL
 * specification map and returned with HTTP 200.</li>
 * <li>Any {@link QuerySanitizationException} short-circuits the pipeline
 * and returns HTTP 400.</li>
 * </ol>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class GraphQLModelController {

    private final DgsReactiveQueryExecutor dgsReactiveQueryExecutor;
    private final QuerySanitizerService sanitizerService;
    private final GraphQLErrorEnhancerService errorEnhancerService;

    /**
     * Executes a GraphQL query after sanitization.
     *
     * @param request the incoming GraphQL request body; must not be {@code null}
     *        and must contain a non-blank {@code query} field
     * @return a {@link Mono} emitting:
     *         <ul>
     *         <li>HTTP 200 with the GraphQL result on success</li>
     *         <li>HTTP 400 with an error message when sanitization fails</li>
     *         </ul>
     */
    @PostMapping(
        path = "/model",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> executeQuery(
        @RequestBody @Valid GraphQLRequest request) {

        log.debug("Received GraphQL request at /model, operationName={}", request.getOperationName());

        return sanitizerService.sanitize(request)
            .flatMap(sanitized -> dgsReactiveQueryExecutor.execute(
                sanitized.getQuery(),
                sanitized.getVariables()))
            .map(result -> {
                log.debug("GraphQL execution completed, errors={}", result.getErrors().size());
                Map<String, Object> spec = errorEnhancerService.enhance(result.toSpecification());
                return ResponseEntity.ok(spec);
            })
            .onErrorResume(QuerySanitizationException.class, ex -> {
                log.warn("Query sanitization failed: {}", ex.getMessage());
                return Mono.just(
                    ResponseEntity.badRequest()
                        .<Map<String, Object>>body(
                            GraphQLErrorBuilder.buildErrorBody(
                                ex.getMessage(), "BAD_REQUEST", 400)));
            });
    }
}
