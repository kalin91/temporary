package com.demo.portfolio.api.config;

import com.demo.portfolio.api.service.GraphQLErrorEnhancerService;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorEnhancerInterceptorTest {

    @Mock
    private GraphQLErrorEnhancerService enhancerService;
    @Mock
    private WebGraphQlRequest request;
    @Mock
    private WebGraphQlInterceptor.Chain chain;
    @Mock
    private WebGraphQlResponse response;
    @Mock
    private WebGraphQlResponse transformed;

    @Test
    void interceptEnhancesErrorsWhenPresent() {
        ErrorEnhancerInterceptor interceptor = new ErrorEnhancerInterceptor(enhancerService);
        GraphQLError error = GraphqlErrorBuilder.newError().message("bad").build();
        ExecutionResult original = ExecutionResultImpl.newExecutionResult().errors(List.of(error)).build();
        ExecutionResult enhanced = ExecutionResultImpl.newExecutionResult().errors(List.of(error)).build();

        when(chain.next(request)).thenReturn(Mono.just(response));
        when(response.getErrors()).thenReturn(List.of(error));
        when(response.getExecutionResult()).thenReturn(original);
        when(enhancerService.enhance(original)).thenReturn(enhanced);
        when(response.transform(any())).thenReturn(transformed);

        assertSame(transformed, interceptor.intercept(request, chain).block());
    }

    @Test
    void interceptReturnsResponseWhenNoErrors() {
        ErrorEnhancerInterceptor interceptor = new ErrorEnhancerInterceptor(enhancerService);
        when(chain.next(request)).thenReturn(Mono.just(response));
        when(response.getErrors()).thenReturn(List.of());

        assertSame(response, interceptor.intercept(request, chain).block());
    }
}
