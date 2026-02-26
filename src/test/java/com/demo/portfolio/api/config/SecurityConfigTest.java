package com.demo.portfolio.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecurityConfig}.
 *
 * <p>Verifies that the in-memory user store is populated correctly, that role assignments
 * follow the expected {@code ROLE_ADMIN} / {@code ROLE_USER} split, and that passwords
 * are BCrypt-encoded and never stored as plain text.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Verifies that {@link SecurityConfig#passwordEncoder()} produces a {@link BCryptPasswordEncoder} instance.
     */
    @Test
    void passwordEncoderBeanIsBCrypt() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
    }

    /**
     * Verifies that the admin principal is assigned both {@code ROLE_ADMIN} and {@code ROLE_USER},
     * allowing it to invoke both queries and mutations.
     */
    @Test
    void adminUserHasAdminAndUserRoles() {
        var service = securityConfig.userDetailsService(
                "admin", "admin123", "user", "user123", encoder);

        UserDetails admin = service.findByUsername("admin").block();
        assertNotNull(admin);
        assertTrue(admin.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(admin.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    /**
     * Verifies that the read-only principal only holds {@code ROLE_USER} and does not
     * receive the elevated {@code ROLE_ADMIN} role.
     */
    @Test
    void regularUserHasOnlyUserRole() {
        var service = securityConfig.userDetailsService(
                "admin", "admin123", "user", "user123", encoder);

        UserDetails user = service.findByUsername("user").block();
        assertNotNull(user);
        assertTrue(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertFalse(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * Verifies that passwords are BCrypt-encoded in the user store and are not stored as plain text,
     * ensuring that the raw credential value is never retained in memory.
     */
    @Test
    void passwordsAreEncodedAndNotStoredAsPlainText() {
        var service = securityConfig.userDetailsService(
                "admin", "admin123", "user", "user123", encoder);

        UserDetails admin = service.findByUsername("admin").block();
        assertNotNull(admin);
        assertNotEquals("admin123", admin.getPassword());
        assertTrue(encoder.matches("admin123", admin.getPassword()));
    }
}
