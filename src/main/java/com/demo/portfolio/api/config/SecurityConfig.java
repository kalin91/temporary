package com.demo.portfolio.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security configuration for the portfolio API.
 *
 * <p>Secures all GraphQL operations using HTTP Basic authentication with two in-memory
 * principals:
 * <ul>
 *   <li>{@code ROLE_USER} – read-only access (queries)</li>
 *   <li>{@code ROLE_ADMIN} – full access including mutations</li>
 * </ul>
 *
 * <p>The following paths are publicly accessible without authentication:
 * <ul>
 *   <li>{@code /actuator/health} and {@code /actuator/info} – liveness / readiness probes</li>
 *   <li>{@code /graphiql/**} – in-browser GraphQL IDE (dev convenience)</li>
 * </ul>
 *
 * <p>CSRF protection is disabled because the API is stateless (no session cookies are issued).
 * Credentials are loaded from {@code application.yml} under the {@code security.users.*} keys,
 * making them easy to override per environment without touching source code.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Configures the reactive HTTP security filter chain.
     *
     * @param http the {@link ServerHttpSecurity} builder provided by Spring Security
     * @return a fully built {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF protection is intentionally disabled: the API is fully stateless and
                // uses HTTP Basic Auth (no session cookies are issued), so CSRF attacks
                // cannot be mounted against it.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/graphiql/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    /**
     * Creates the in-memory user store with two demo principals.
     *
     * <p><strong>Note:</strong> These are demo credentials intended for portfolio / local-dev use.
     * In a production deployment replace this bean with a persistent user store and proper
     * secret management.
     *
     * @param adminUsername the username for the admin principal (from {@code application.yml})
     * @param adminPassword the plain-text password for the admin principal
     * @param userUsername  the username for the read-only principal
     * @param userPassword  the plain-text password for the read-only principal
     * @param encoder       the {@link PasswordEncoder} used to hash the supplied passwords
     * @return a {@link MapReactiveUserDetailsService} holding both principals
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            @Value("${security.users.admin.username}") String adminUsername,
            @Value("${security.users.admin.password}") String adminPassword,
            @Value("${security.users.user.username}") String userUsername,
            @Value("${security.users.user.password}") String userPassword,
            PasswordEncoder encoder) {

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username(userUsername)
                .password(encoder.encode(userPassword))
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(admin, user);
    }

    /**
     * Provides a {@link BCryptPasswordEncoder} for hashing in-memory user passwords.
     *
     * @return the configured {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
