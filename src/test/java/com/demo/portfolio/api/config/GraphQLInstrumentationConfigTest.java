package com.demo.portfolio.api.config;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GraphQLInstrumentationConfigTest {

    @Test
    void maxQueryComplexityInstrumentationCreatesBean() {
        GraphQLInstrumentationConfig config = new GraphQLInstrumentationConfig();

        MaxQueryComplexityInstrumentation instrumentation = config.maxQueryComplexityInstrumentation();

        assertNotNull(instrumentation);
    }
}
