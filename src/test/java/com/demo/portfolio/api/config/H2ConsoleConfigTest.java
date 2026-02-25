package com.demo.portfolio.api.config;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class H2ConsoleConfigTest {

    @Test
    void startInitializesServerAndStopShutsItDown() throws Exception {
        H2ConsoleConfig config = new H2ConsoleConfig();

        config.start();

        Field field = H2ConsoleConfig.class.getDeclaredField("webServer");
        field.setAccessible(true);
        Object server = field.get(config);
        assertNotNull(server);

        config.stop();
    }

    @Test
    void stopHandlesNullServer() {
        H2ConsoleConfig config = new H2ConsoleConfig();
        config.stop();
    }

    @Test
    void stopCallsServerStopWhenPresent() throws Exception {
        H2ConsoleConfig config = new H2ConsoleConfig();
        Server server = mock(Server.class);

        Field field = H2ConsoleConfig.class.getDeclaredField("webServer");
        field.setAccessible(true);
        field.set(config, server);

        config.stop();

        verify(server).stop();
    }
}
