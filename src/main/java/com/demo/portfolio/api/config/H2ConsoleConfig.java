package com.demo.portfolio.api.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.sql.SQLException;

/**
 * Configuration class for H2 Database Console.
 * 
 * This class manages the lifecycle of the H2 web console server, enabling
 * database inspection and management during application runtime.
 * The console is accessible via HTTP on port 8082.
 * 
 */
@Configuration
public class H2ConsoleConfig {

    /**
     * The H2 web server instance.
     */
    private Server webServer;

    /**
     * Starts the H2 web console server on port 8082.
     * 
     * This method is automatically invoked when the Spring application context
     * is refreshed.
     * 
     * @throws SQLException if an error occurs while starting the H2 server
     */
    @EventListener(ContextRefreshedEvent.class)
    public void start() throws SQLException {
        this.webServer = Server.createWebServer("-webPort", "8082").start();
    }

    /**
     * Stops the H2 web console server gracefully.
     * 
     * This method is automatically invoked when the Spring application context
     * is closed, ensuring proper resource cleanup and server shutdown.
     */
    @EventListener(ContextClosedEvent.class)
    public void stop() {
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }
}
