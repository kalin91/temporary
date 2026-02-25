package com.demo.portfolio.api.config;

import com.demo.portfolio.api.service.GraphQLErrorEnhancerService;
import graphql.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Response-side {@link WebGraphQlInterceptor} that enriches GraphQL error
 * messages after query execution.
 *
 * <h2>Purpose</h2>
 * <p>
 * GraphQL variable-coercion errors (e.g. an invalid enum value) are generated
 * by the {@code graphql-java} engine <em>before</em> any data fetcher runs.
 * Because they bypass the {@code DataFetcherExceptionHandler}, those errors
 * carry only the engine's default message — which does not list valid values.
 * </p>
 *
 * <p>
 * This interceptor delegates to {@link GraphQLErrorEnhancerService} on the
 * <strong>response path</strong> to rewrite enum-coercion error messages so
 * they include the valid enum values from the live schema.
 * </p>
 *
 * <h2>Pipeline position</h2>
 * <p>
 * Runs <em>after</em> the GraphQL engine has produced an
 * {@code ExecutionResult}. Errors that do not match the enhancement pattern
 * pass through unmodified.
 * </p>
 *
 * @see GraphQLErrorEnhancerService
 * @see SanitizingInterceptor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorEnhancerInterceptor implements WebGraphQlInterceptor {

    private final GraphQLErrorEnhancerService enhancerService;

    /**
     * Intercepts the GraphQL response and enriches any enum-coercion errors
     * with valid values from the schema.
     *
     * <p>
     * The request is forwarded to the next handler in the chain without
     * modification. Once the response is available, the
     * {@link GraphQLErrorEnhancerService#enhance} method is applied to the
     * {@code ExecutionResult} via
     * {@link WebGraphQlResponse#transform(java.util.function.Consumer)}.
     * </p>
     *
     * @param request the incoming {@link WebGraphQlRequest}; passed through
     *                unmodified
     * @param chain   the interceptor chain to delegate execution to
     * @return a {@link Mono} emitting the (possibly enhanced)
     *         {@link WebGraphQlResponse}
     */
    @Override
    @NonNull
    public Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request,
                                              @NonNull Chain chain) {
        return Objects.requireNonNull(chain.next(request)
                .map(response -> {
                    if (!response.getErrors().isEmpty()) {
                        log.debug("Response contains {} error(s); applying enum error enhancement",
                                response.getErrors().size());
                        ExecutionResult enhanced = enhancerService.enhance(
                                response.getExecutionResult());
                        return response.transform(builder ->
                                builder.errors(enhanced.getErrors()));
                    }
                    return response;
                }));
    }
}
