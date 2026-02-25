package com.demo.portfolio.api;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class PortfolioApiApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            PortfolioApiApplication.main(new String[]{"--demo"});

            springApplication.verify(() -> SpringApplication.run(PortfolioApiApplication.class, new String[]{"--demo"}));
        }
    }
}
