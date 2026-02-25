package com.demo.portfolio.api.service;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.execution.GraphQlSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphQLErrorEnhancerServiceTest {

    @Mock
    private GraphQlSource graphQlSource;
    @Mock
    private GraphQLSchema graphQLSchema;

    @InjectMocks
    private GraphQLErrorEnhancerService service;

    @Test
    void enhanceReturnsOriginalWhenNoErrors() {
        ExecutionResult result = ExecutionResultImpl.newExecutionResult().data("ok").build();

        assertSame(result, service.enhance(result));
    }

    @Test
    void enhanceRewritesEnumErrorsWhenEnumExists() {
        GraphQLError error = GraphqlErrorBuilder.newError()
                .message("Invalid input for enum 'OrderStatus'. No value found for name 'BAD'")
                .build();
        ExecutionResult result = ExecutionResultImpl.newExecutionResult().data("ok").errors(List.of(error)).build();

        GraphQLEnumType enumType = GraphQLEnumType.newEnum()
                .name("OrderStatus")
                .value(new GraphQLEnumValueDefinition("PENDING", null))
                .value(new GraphQLEnumValueDefinition("SHIPPED", null))
                .build();
        when(graphQlSource.schema()).thenReturn(graphQLSchema);
        when(graphQLSchema.getType("OrderStatus")).thenReturn(enumType);

        ExecutionResult enhanced = service.enhance(result);

        assertNotEquals(error.getMessage(), enhanced.getErrors().getFirst().getMessage());
        assertTrue(enhanced.getErrors().getFirst().getMessage().contains("Valid values are"));
    }

    @Test
    void enhanceKeepsOriginalWhenPatternDoesNotMatch() {
        GraphQLError error = GraphqlErrorBuilder.newError().message("Other error").build();
        ExecutionResult result = ExecutionResultImpl.newExecutionResult().errors(List.of(error)).build();

        ExecutionResult enhanced = service.enhance(result);

        assertEquals("Other error", enhanced.getErrors().getFirst().getMessage());
    }
}
