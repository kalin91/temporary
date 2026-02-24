package com.demo.portfolio.api.config;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.service.QuerySanitizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Request-side {@link WebGraphQlInterceptor} that validates and sanitizes
 * incoming GraphQL queries <em>before</em> they reach the DGS data fetchers.
 *
 * <h2>Pipeline position</h2>
 * <p>
 * Runs on the <strong>request path</strong>. The incoming
 * {@link WebGraphQlRequest} is mapped to a {@link GraphQLRequest} DTO and
 * passed through {@link QuerySanitizerService#sanitize}. If sanitization
 * succeeds, the original request is forwarded to the next handler. If it
 * fails, a {@link com.demo.portfolio.api.exception.QuerySanitizationException}
 * propagates as a {@code Mono.error}, which the framework converts into a
 * standard GraphQL error response.
 * </p>
 *
 * @see QuerySanitizerService
 * @see ErrorEnhancerInterceptor
 */
@Component
@RequiredArgsConstructor
public class SanitizingInterceptor implements WebGraphQlInterceptor {

    private final QuerySanitizerService sanitizerService;

    /**
     * Intercepts the incoming request, applies sanitization, and forwards it
     * to the next handler in the chain if validation passes.
     *
     * @param request the incoming {@link WebGraphQlRequest}
     * @param chain   the interceptor chain to delegate execution to
     * @return a {@link Mono} emitting the {@link WebGraphQlResponse}, or a
     *         failed {@code Mono} if sanitization rejects the query
     */
    @Override
    @NonNull
    public Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request, @NonNull Chain chain) {
        // Map WebGraphQlRequest to our internal DTO for sanitization
        GraphQLRequest dto = GraphQLRequest.builder()
                .query(request.getDocument())
                .operationName(request.getOperationName())
                .variables(request.getVariables())
                .build();

        // Perform sanitization check
        return sanitizerService.sanitize(dto)
                .flatMap(sanitizedDto -> 
                    // If sanitization passes, proceed with the original request
                    chain.next(request)
                );
    }
}
