package io.github.huahill.vadmin.springjdbc;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores the permission catalog and the initial administrator in a host-migrated IAM schema.
 */
final class JdbcIamStore {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcIamTables tables;
    private final UuidStorage uuidStorage;

    JdbcIamStore(JdbcTemplate jdbcTemplate, String tablePrefix, UuidStorage uuidStorage) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuidStorage = Objects.requireNonNull(uuidStorage);
    }

    public void synchronizePermissions(Collection<String> permissionCodes) {
        for (String code : permissionCodes) {
            insertPermissionIfMissing(code);
        }
    }

    public boolean hasAnyUser() {
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tables.users(), Long.class);
        return userCount != null && userCount > 0;
    }

    public int grantMissingPermissionsToAdministrator(Collection<String> permissionCodes) {
        List<String> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM " + tables.roles() + " WHERE code = 'administrator' FOR UPDATE", String.class);
        int granted = 0;
        for (String roleId : roleIds) {
            int added = grantMissingPermissions(roleId, permissionCodes);
            if (added > 0) {
                jdbcTemplate.update("UPDATE " + tables.users() + " SET auth_version = auth_version + 1 "
                        + "WHERE id IN (SELECT user_id FROM " + tables.userRoles() + " WHERE role_id = ?)",
                        uuidStorage.parameter(UUID.fromString(roleId)));
            }
            granted += added;
        }
        return granted;
    }

    public int bootstrapAdministrator(String passwordHash, Collection<String> permissionCodes) {
        Object administratorUserId = uuidStorage.parameter(UUID.randomUUID());
        UUID administratorRoleId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO " + tables.users() + " (id, username, password_hash, enabled, auth_version) VALUES (?, 'admin', ?, TRUE, 0)",
                administratorUserId, passwordHash);
        jdbcTemplate.update("INSERT INTO " + tables.roles() + " (id, code) VALUES (?, 'administrator')",
                uuidStorage.parameter(administratorRoleId));
        jdbcTemplate.update("INSERT INTO " + tables.userRoles() + " (user_id, role_id) VALUES (?, ?)",
                administratorUserId, uuidStorage.parameter(administratorRoleId));
        return grantMissingPermissions(administratorRoleId.toString(), permissionCodes);
    }

    private void insertPermissionIfMissing(String code) {
        Long existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tables.permissions() + " WHERE code = ?", Long.class, code);
        if (existing != null && existing > 0) {
            return;
        }
        // Let a duplicate roll back the whole transaction before the initializer retries;
        // PostgreSQL does not permit continuing a transaction after a constraint violation.
        jdbcTemplate.update(
                "INSERT INTO " + tables.permissions() + " (id, code, system_managed, created_at) VALUES (?, ?, TRUE, CURRENT_TIMESTAMP)",
                uuidStorage.parameter(UUID.randomUUID()), code);
    }

    private int grantMissingPermissions(String roleId, Collection<String> permissionCodes) {
        List<String> codes = List.copyOf(permissionCodes);
        if (codes.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", codes.stream().map(code -> "?").toList());
        List<Object> arguments = new ArrayList<>();
        arguments.add(uuidStorage.parameter(UUID.fromString(roleId)));
        arguments.addAll(codes);
        arguments.add(uuidStorage.parameter(UUID.fromString(roleId)));
        return jdbcTemplate.update(
                "INSERT INTO " + tables.rolePermissions() + " (role_id, permission_id) "
                        + "SELECT ?, p.id FROM " + tables.permissions() + " p WHERE p.code IN (" + placeholders + ") "
                        + "AND NOT EXISTS (SELECT 1 FROM " + tables.rolePermissions() + " rp "
                        + "WHERE rp.role_id = ? AND rp.permission_id = p.id)",
                arguments.toArray());
    }
}
