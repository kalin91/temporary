package com.demo.portfolio.api.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.sql.SQLException;

/**
 * Configuration to start H2 Console in a WebFlux application.
 */
@Configuration
public class H2ConsoleConfig {

    private Server webServer;

    @EventListener(ContextRefreshedEvent.class)
    public void start() throws SQLException {
        this.webServer = Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
    }

    @EventListener(ContextClosedEvent.class)
    public void stop() {
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }
}
