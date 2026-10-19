package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Portable system-management storage; schema migration and transaction ownership stay with the host. */
public final class JdbcAdministrationStore implements AdministrationStore {
    private final JdbcTemplate jdbc;
    private final JdbcIamTables tables;
    private final UuidStorage uuids;

    public JdbcAdministrationStore(JdbcTemplate jdbc, String tablePrefix, UuidStorage uuids) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc);
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuids = java.util.Objects.requireNonNull(uuids);
    }

    @Override
    public PagedResult<UserRow> users(PagedQuery query) {
        String filter = "%" + query.filters().getOrDefault("q", "").strip() + "%";
        String where = " FROM " + tables.users() + " WHERE LOWER(username) LIKE LOWER(?)";
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, filter);
        String sort = switch (query.sortField()) {
            case "enabled" -> "enabled";
            case "authVersion" -> "auth_version";
            default -> "username";
        };
        var rows = jdbc.query("SELECT id, username, enabled, auth_version" + where + " ORDER BY " + sort
                        + direction(query) + ", id LIMIT ? OFFSET ?",
                (rs, row) -> new UserRow(UUID.fromString(rs.getString("id")), rs.getString("username"),
                        rs.getBoolean("enabled"), rs.getLong("auth_version")), filter, query.pageSize(), offset(query));
        return new PagedResult<>(rows, total);
    }

    @Override
    public List<RoleRow> roles() {
        return roleRows(jdbc.query("SELECT id, code FROM " + tables.roles() + " ORDER BY code, id",
                (rs, row) -> new RoleRow(UUID.fromString(rs.getString("id")), rs.getString("code"), 0, List.of())));
    }

    @Override
    public PagedResult<RoleRow> roles(PagedQuery query) {
        long total = count(tables.roles());
        var rows = jdbc.query("SELECT id, code FROM " + tables.roles() + " ORDER BY code" + direction(query)
                        + ", id LIMIT ? OFFSET ?",
                (rs, row) -> new RoleRow(UUID.fromString(rs.getString("id")), rs.getString("code"), 0, List.of()),
                query.pageSize(), offset(query));
        return new PagedResult<>(roleRows(rows), total);
    }

    private List<RoleRow> roleRows(List<RoleRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<String>> codes = new HashMap<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(rows.size(), "?"));
        jdbc.query("SELECT rp.role_id, p.code FROM " + tables.rolePermissions() + " rp JOIN " + tables.permissions()
                        + " p ON p.id = rp.permission_id WHERE rp.role_id IN (" + placeholders + ") ORDER BY p.code",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> codes
                        .computeIfAbsent(UUID.fromString(rs.getString("role_id")), ignored -> new ArrayList<>())
                        .add(rs.getString("code")), rows.stream().map(row -> uuids.parameter(row.id())).toArray());
        return rows.stream().map(row -> {
            List<String> permissions = codes.getOrDefault(row.id(), List.of());
            return new RoleRow(row.id(), row.code(), permissions.size(), permissions);
        }).toList();
    }

    @Override
    public PagedResult<PermissionRow> permissions(PagedQuery query) {
        var rows = jdbc.query("SELECT code, system_managed FROM " + tables.permissions() + " ORDER BY code"
                        + direction(query) + " LIMIT ? OFFSET ?",
                (rs, row) -> new PermissionRow(rs.getString("code"), rs.getBoolean("system_managed")),
                query.pageSize(), offset(query));
        return new PagedResult<>(rows, count(tables.permissions()));
    }

    @Override
    public PagedResult<AuditRow> audit(PagedQuery query) {
        var rows = jdbc.query("SELECT occurred_at, action_code, target_type, target_id, outcome, correlation_id FROM "
                        + tables.audit() + " ORDER BY occurred_at DESC, id LIMIT ? OFFSET ?",
                (rs, row) -> new AuditRow(rs.getTimestamp("occurred_at").toInstant(), rs.getString("action_code"),
                        rs.getString("target_type"), rs.getString("target_id"), rs.getString("outcome"),
                        rs.getString("correlation_id")), query.pageSize(), offset(query));
        return new PagedResult<>(rows, count(tables.audit()));
    }

    @Override
    public Set<UUID> userRoleIds(UUID userId) {
        return Set.copyOf(jdbc.query("SELECT role_id FROM " + tables.userRoles() + " WHERE user_id = ?",
                (rs, row) -> UUID.fromString(rs.getString("role_id")), uuids.parameter(userId)));
    }

    @Override
    public boolean userExists(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tables.users() + " WHERE id = ?",
                Long.class, uuids.parameter(userId)) > 0;
    }

    @Override
    public boolean insertUser(UUID id, String username, String passwordHash) {
        try {
            jdbc.update("INSERT INTO " + tables.users()
                    + " (id, username, password_hash, enabled, auth_version) VALUES (?, ?, ?, TRUE, 0)",
                    uuids.parameter(id), username, passwordHash);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Override
    public boolean insertRole(UUID id, String code) {
        try {
            jdbc.update("INSERT INTO " + tables.roles() + " (id, code) VALUES (?, ?)", uuids.parameter(id), code);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Override
    public void setEnabled(UUID userId, boolean enabled) {
        jdbc.update("UPDATE " + tables.users() + " SET enabled = ?, auth_version = auth_version + 1 WHERE id = ?",
                enabled, uuids.parameter(userId));
    }

    @Override
    public void replaceUserRoles(UUID userId, Set<UUID> roleIds) {
        jdbc.update("DELETE FROM " + tables.userRoles() + " WHERE user_id = ?", uuids.parameter(userId));
        roleIds.forEach(roleId -> jdbc.update("INSERT INTO " + tables.userRoles() + " (user_id, role_id) VALUES (?, ?)",
                uuids.parameter(userId), uuids.parameter(roleId)));
        jdbc.update("UPDATE " + tables.users() + " SET auth_version = auth_version + 1 WHERE id = ?", uuids.parameter(userId));
    }

    @Override
    public void lockAdministratorRole() {
        jdbc.queryForList("SELECT id FROM " + tables.roles() + " WHERE code = 'administrator' FOR UPDATE");
    }

    @Override
    public boolean isLastEnabledAdministrator(UUID userId) {
        var administrators = jdbc.query("SELECT u.id FROM " + tables.users() + " u JOIN " + tables.userRoles()
                        + " ur ON ur.user_id = u.id JOIN " + tables.roles()
                        + " r ON r.id = ur.role_id WHERE r.code = 'administrator' AND u.enabled = TRUE",
                (rs, row) -> UUID.fromString(rs.getString("id")));
        return administrators.size() == 1 && administrators.getFirst().equals(userId);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private static long offset(PagedQuery query) { return Math.multiplyExact((long) query.page(), query.pageSize()); }
    private static String direction(PagedQuery query) { return query.ascending() ? " ASC" : " DESC"; }
}
