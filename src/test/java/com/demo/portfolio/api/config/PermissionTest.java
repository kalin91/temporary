package com.demo.portfolio.api.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Permission} enum.
 *
 * <p>Verifies that the bitmask-based {@link Permission#fromMask(int)} method correctly
 * resolves permission sets for each role profile, and that the SpEL expression constants
 * used in {@code @PreAuthorize} annotations contain the expected role names.
 */
class PermissionTest {

    /**
     * Verifies that a bitmask of 7 (1 + 2 + 4 = ADMIN + WRITER + READER) returns all three permissions.
     */
    @Test
    void fromMask7ReturnsAllPermissions() {
        Set<Permission> perms = Permission.fromMask(7);
        assertTrue(perms.contains(Permission.ADMIN));
        assertTrue(perms.contains(Permission.WRITER));
        assertTrue(perms.contains(Permission.READER));
    }

    /**
     * Verifies that a bitmask of 6 (2 + 4 = WRITER + READER) returns only WRITER and READER.
     */
    @Test
    void fromMask6ReturnsWriterAndReader() {
        Set<Permission> perms = Permission.fromMask(6);
        assertFalse(perms.contains(Permission.ADMIN));
        assertTrue(perms.contains(Permission.WRITER));
        assertTrue(perms.contains(Permission.READER));
    }

    /**
     * Verifies that a bitmask of 4 (READER only) returns only READER.
     */
    @Test
    void fromMask4ReturnsReaderOnly() {
        Set<Permission> perms = Permission.fromMask(4);
        assertFalse(perms.contains(Permission.ADMIN));
        assertFalse(perms.contains(Permission.WRITER));
        assertTrue(perms.contains(Permission.READER));
    }

    /**
     * Verifies that a bitmask of 0 returns an empty permission set.
     */
    @Test
    void fromMask0ReturnsEmptySet() {
        Set<Permission> perms = Permission.fromMask(0);
        assertTrue(perms.isEmpty());
    }

    /**
     * Verifies the SpEL expression constant for {@code ROLE_ADMIN} contains the expected role check.
     */
    @Test
    void roleAdminConstantContainsAdminRole() {
        assertEquals("hasRole('ADMIN')", Permission.ROLE_ADMIN);
    }

    /**
     * Verifies the SpEL expression constant for {@code ROLE_WRITER} contains the expected role check.
     */
    @Test
    void roleWriterConstantContainsWriterRole() {
        assertEquals("hasRole('WRITER')", Permission.ROLE_WRITER);
    }

    /**
     * Verifies the SpEL expression constant for {@code ROLE_READER} contains the expected role check.
     */
    @Test
    void roleReaderConstantContainsReaderRole() {
        assertEquals("hasRole('READER')", Permission.ROLE_READER);
    }

    /**
     * Verifies that each {@link Permission} enum constant has the correct bitmask value,
     * matching the documented scheme: ADMIN=1, WRITER=2, READER=4.
     */
    @Test
    void permissionBitmaskValues() {
        assertEquals(1, Permission.ADMIN.bit());
        assertEquals(2, Permission.WRITER.bit());
        assertEquals(4, Permission.READER.bit());
    }
}
