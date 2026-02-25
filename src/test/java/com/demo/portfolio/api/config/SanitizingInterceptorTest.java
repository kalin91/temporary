package com.demo.portfolio.api.config;

import com.demo.portfolio.api.dto.GraphQLRequest;
import com.demo.portfolio.api.service.QuerySanitizerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanitizingInterceptorTest {

    @Mock
    private QuerySanitizerService sanitizerService;
    @Mock
    private WebGraphQlRequest request;
    @Mock
    private WebGraphQlInterceptor.Chain chain;
    @Mock
    private WebGraphQlResponse response;

    @Test
    void interceptSanitizesThenDelegates() {
        SanitizingInterceptor interceptor = new SanitizingInterceptor(sanitizerService);
        when(request.getDocument()).thenReturn("query { customers { id } }");
        when(request.getOperationName()).thenReturn("op");
        when(request.getVariables()).thenReturn(Map.of("k", "v"));
        when(sanitizerService.sanitize(any(GraphQLRequest.class)))
                .thenReturn(Mono.just(GraphQLRequest.builder().query("query { customers { id } }").build()));
        when(chain.next(request)).thenReturn(Mono.just(response));

        WebGraphQlResponse result = interceptor.intercept(request, chain).block();

        assertSame(response, result);
    }
}
