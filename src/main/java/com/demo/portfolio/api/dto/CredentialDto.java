package com.demo.portfolio.api.dto;

/**
 * Credential pair used to represent a single API principal loaded from the
 * {@code API_CREDENTIALS_JSON} environment variable.
 *
 * <p>
 * Each entry in the JSON map is deserialized into one of these records.
 * The map key indicates the role profile ({@code admin}, {@code writer}, or {@code reader}),
 * while this record holds the actual username/password pair for that profile.
 * </p>
 *
 * @param user the username for this principal
 * @param pass the plain-text password for this principal; it is BCrypt-encoded before
 *        being stored in the in-memory user store
 * @param permissions the permission level for this principal, where 4 points are for reading,
 *        2 are for writing, and 1 is for deleting; the sum of these values determines the roles assigned to the principal
 *        (e.g. a value of 6 corresponds to read/write permissions and thus gets both ROLE_READER and ROLE_WRITER)
 */
public record CredentialDto(String user, String pass, int permissions) {
}
