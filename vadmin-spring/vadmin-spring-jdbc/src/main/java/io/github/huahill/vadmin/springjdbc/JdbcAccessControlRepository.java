package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.access.Permission;
import io.github.huahill.vadmin.platform.access.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** JDBC permission-grant adapter for a host-managed IAM schema. */
public final class JdbcAccessControlRepository implements AccessControlRepository {
    private final JdbcTemplate jdbc;
    private final JdbcIamTables tables;
    private final UuidStorage uuids;

    public JdbcAccessControlRepository(JdbcTemplate jdbc, String tablePrefix, UuidStorage uuids) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc);
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuids = java.util.Objects.requireNonNull(uuids);
    }

    @Override
    public Optional<Role> findRoleByCode(String code) {
        return jdbc.query("SELECT id, code FROM " + tables.roles() + " WHERE code = ?",
                (rs, row) -> new Role(UUID.fromString(rs.getString("id")), rs.getString("code")), code)
                .stream().findFirst();
    }

    @Override
    public Optional<Permission> findPermissionByCode(PermissionCode code) {
        return jdbc.query("SELECT id, code FROM " + tables.permissions() + " WHERE code = ?",
                (rs, row) -> new Permission(UUID.fromString(rs.getString("id")), PermissionCode.of(rs.getString("code"))),
                code.value()).stream().findFirst();
    }

    @Override
    public void grantPermission(UUID roleId, UUID permissionId) {
        jdbc.queryForList("SELECT id FROM " + tables.roles() + " WHERE id = ? FOR UPDATE", uuids.parameter(roleId));
        jdbc.update("INSERT INTO " + tables.rolePermissions() + " (role_id, permission_id) SELECT ?, ? "
                        + "WHERE NOT EXISTS (SELECT 1 FROM " + tables.rolePermissions()
                        + " WHERE role_id = ? AND permission_id = ?)",
                uuids.parameter(roleId), uuids.parameter(permissionId), uuids.parameter(roleId), uuids.parameter(permissionId));
    }

    @Override
    public void incrementAuthVersionForRole(UUID roleId) {
        jdbc.update("UPDATE " + tables.users() + " SET auth_version = auth_version + 1 WHERE id IN (SELECT user_id FROM "
                + tables.userRoles() + " WHERE role_id = ?)", uuids.parameter(roleId));
    }
}
