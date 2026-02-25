package com.demo.portfolio.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ITKarateRunner {

    @LocalServerPort
    private int port;

    @Test
    void testParallel() {
        Results results = Runner.path("classpath:")
            .systemProperty("demo.server.port", port + "")
            .parallel(5);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
