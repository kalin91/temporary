package com.demo.portfolio.api.config;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permission bitmask constants for the API's role-based access control model.
 *
 * <p>Each enum constant represents a single permission bit:
 * <ul>
 *   <li>{@link #ADMIN} (bit 1) – delete operations</li>
 *   <li>{@link #WRITER} (bit 2) – create and update operations</li>
 *   <li>{@link #READER} (bit 4) – read-only query operations</li>
 * </ul>
 *
 * <p>Permission sets are stored as additive bitmasks in the credential JSON supplied via
 * {@code API_CREDENTIALS_JSON}. For example, an {@code admin} profile with
 * {@code "permissions": 7} (= 1 + 2 + 4) receives all three roles.
 *
 * <p>The {@link #ROLE_ADMIN}, {@link #ROLE_WRITER}, and {@link #ROLE_READER} string constants
 * are SpEL expressions suitable for use directly in {@code @PreAuthorize} annotations.
 */
public enum Permission {

    /** Delete permission – required for {@code delete*} mutations. */
    ADMIN(1),

    /** Create/update permission – required for {@code create*} and {@code update*} mutations. */
    WRITER(2),

    /** Read permission – required for all query operations. */
    READER(4);

    /** SpEL expression for {@code @PreAuthorize} on delete mutations. */
    public static final String ROLE_ADMIN  = "hasRole('ADMIN')";

    /** SpEL expression for {@code @PreAuthorize} on create and update mutations. */
    public static final String ROLE_WRITER = "hasRole('WRITER')";

    /** SpEL expression for {@code @PreAuthorize} on query methods. */
    public static final String ROLE_READER = "hasRole('READER')";

    private final int bit;

    Permission(int bit) {
        this.bit = bit;
    }

    /**
     * Returns the bitmask value of this permission.
     *
     * @return the bitmask value (1 for ADMIN, 2 for WRITER, 4 for READER)
     */
    public int bit() {
        return bit;
    }

    /**
     * Resolves the set of {@link Permission} values present in the given bitmask.
     *
     * @param mask the integer bitmask (e.g. {@code 7} for full access, {@code 4} for read-only)
     * @return the set of permissions whose bits are set in {@code mask}; never {@code null}
     */
    public static Set<Permission> fromMask(int mask) {
        EnumSet<Permission> set = EnumSet.noneOf(Permission.class);
        for (Permission p : values()) {
            if ((mask & p.bit) != 0) {
                set.add(p);
            }
        }
        return set;
    }
}

